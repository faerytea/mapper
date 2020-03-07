/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Processor.
 *
 * Mapper Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapper Processor.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.processor

import com.gitlab.faerytea.mapper.converters.*
import com.gitlab.faerytea.mapper.gen.AdapterInfo
import com.gitlab.faerytea.mapper.gen.ConverterData
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*
import javax.lang.model.element.Modifier.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleElementVisitor8
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass

object ConverterVisitor : SimpleElementVisitor8<ConverterData?, ProcessingEnvironment>() {
    private val forbiddenModifiers: EnumSet<Modifier> = EnumSet.of(PRIVATE, STATIC, ABSTRACT, DEFAULT)
    private val encDecConverters = listOf<KClass<*>>(
            Converter::class,
            IntToIntConverter::class,
            DoubleToDoubleConverter::class,
            LongToLongConverter::class
    )
    private val toXConverters = listOf<KClass<*>>(
            BooleanConverter::class,
            BooleanIntConverter::class,
            DoubleConverter::class,
            DoubleLongConverter::class,
            IntConverter::class,
            LongConverter::class
    )

    override fun visitTypeParameter(e: TypeParameterElement, p: ProcessingEnvironment): ConverterData? {
        return extractConverter(e, e.asType(), p)
    }

    override fun visitExecutable(e: ExecutableElement, p: ProcessingEnvironment): ConverterData? = when (e.parameters.size) {
        0 -> extractConverter(e, e.returnType, p) // getter
        1 -> extractConverter(e, e.parameters[0].asType(), p) // simple setter
        else -> throw IllegalArgumentException("$e does not looks like getter or simple setter")
    }

    override fun visitVariable(e: VariableElement, p: ProcessingEnvironment): ConverterData? {
        return extractConverter(e, e.asType(), p)
    }

    private fun extractConverter(e: Element, tp: TypeMirror, env: ProcessingEnvironment): ConverterData? = e.getAnnotation(Convert::class.java)?.let {
        buildConverterData(env, tp, e, it)
    }

    internal fun buildConverterData(env: ProcessingEnvironment, tp: TypeMirror, e: Element, annotation: Convert): ConverterData? {
        val elements = env.elementUtils
        val types = env.typeUtils
        val converterElement = elements.getTypeElement(try {
            @Suppress("INACCESSIBLE_TYPE")
            annotation.value.java.name
        } catch (e: MirroredTypeException) {
            e.typeMirror.toString()
        })
        val converterType = types.erasure(converterElement.asType())
        if (encDecConverters.any { types.isAssignable(converterType, types.erasure(elements.mirror(it))) }) {
            // encode / decode
            val methods = converterElement.methods()
            Log.note { "all methods: $methods" }
            // <type> <name>(<type> <param>)
            val rightSignature = methods.filter { it.parameters.printIt().size == 1 && (it.modifiers.printIt() intersect forbiddenModifiers).printIt().isEmpty() && it.returnType.kind.printIt() != TypeKind.VOID }
            Log.note { "right signature: $rightSignature" }
            val encode = rightSignature.find { it.simpleName.toString().printIt() == "encode" }
            val decode = rightSignature.find { it.simpleName.toString().printIt() == "decode" }
            Log.note { "enc: $encode; dec: $decode" }
            if (encode == null || decode == null) {
                env.messager.printMessage(Diagnostic.Kind.ERROR, "@Convert annotation provides converter which lacks encode / decode methods", e)
                return null // not all necessary methods found
            }
            // check types
            return findTypes(encode, decode, tp, e, env, annotation.reversed)?.let { (from, to) ->
                ConverterData(AdapterInfo(converterElement.qualifiedName.toString()), from, to)
            }
        }
        if (toXConverters.any { types.isAssignable(converterType, types.erasure(elements.mirror(it))) }) {
            // toX
            val methods = converterElement.methods()
            // <type> <name>(<type> <param>)
            val rightSignature = methods.filter { it.parameters.size == 1 && (it.modifiers intersect forbiddenModifiers).isEmpty() && it.returnType.kind != TypeKind.VOID }
            val converterData = if (tp.kind.isPrimitive) {
                // our type is primitive
                val toNameMethods = rightSignature.filter { it.simpleName.toString() == ConverterData.nameByKind("to", tp.kind)!! }
                Log.note { "right signature $rightSignature" }
                Log.note { "toNameMethods $toNameMethods" }
                if (toNameMethods.size == 1) {
                    val oppositeTp = toNameMethods[0].parameters[0].asType()
                    val reverseConversionName =
                            if (oppositeTp.kind.isPrimitive)
                            // so we need "toOppositeTp" with tp as argument
                                ConverterData.nameByKind("to", oppositeTp.kind)!!
                            else
                            // so we need "fromTp" with oppositeTp as ret type
                                ConverterData.nameByKind("from", tp.kind)!!
                    Log.note { "reverse conversion name: $reverseConversionName" }
                    ConverterData(AdapterInfo(converterElement.qualifiedName.toString()), oppositeTp, tp).takeIf {
                        (rightSignature.filter {
                            Log.note { "filtering: $it: ${it.simpleName}, ${it.parameters[0].asType()}, our is $tp and we have equality ${types.isSameType(it.parameters[0].asType(), tp)}" }
                            it.simpleName.toString() == reverseConversionName && types.isSameType(it.parameters[0].asType(), tp)
                        }.also { Log.note { "filtered: $it" } }.size == 1)
                    }
                } else {
                    null
                }
            } else {
                // opposite type is primitive, our is not
                val tpCreators = rightSignature.filter { it.simpleName.startsWith("from") && types.isSameType(tp, it.returnType) }
                ConverterData(AdapterInfo(converterElement.qualifiedName.toString()), tpCreators[0].parameters[0].asType(), tp).takeIf {
                    tpCreators.size == 1
                }
            }
            return if (converterData == null) {
                env.messager.printMessage(Diagnostic.Kind.ERROR, "@Convert annotation provides ambiguous or invalid converter", e)
                null
            } else {
                converterData
            }
        }
        env.messager.printMessage(
                Diagnostic.Kind.ERROR,
                """
                @Convert annotation provides something that is not a converter. 
                Valid converters must implement one of ${(encDecConverters + toXConverters).map { it.java.simpleName }}
                """.trimIndent(),
                e)
        return null
    }

    private fun findTypes(encode: ExecutableElement,
                          decode: ExecutableElement,
                          javaTp: TypeMirror,
                          onElement: Element,
                          env: ProcessingEnvironment,
                          reversed: Boolean = false): Pair<TypeMirror, TypeMirror>? {
        if (reversed) return findTypes(decode, encode, javaTp, onElement, env)?.let { it.second to it.first }
        val types = env.typeUtils
        fun say(msg: String) = env.messager.printMessage(Diagnostic.Kind.ERROR, msg, onElement)
        fun notAssignable(method: ExecutableElement, what: TypeMirror, to: TypeMirror) = "${method.simpleName} of converter: $what is is not assignable to $to"
        var correct = true
        // ret types
        if (!types.isAssignable(decode.returnType, javaTp)) {
            say(notAssignable(decode, decode.returnType, javaTp))
            correct = false
        }
        if (!types.isAssignable(encode.parameters[0].asType(), javaTp)) {
            say(notAssignable(encode, javaTp, encode.parameters[0].asType()))
            correct = false
        }
        if (!correct) return null
        // common supertype
        return types.commonSuperType(decode.parameters[0].asType(), encode.returnType)?.let {
            it to javaTp
        }
    }
}

private infix fun <T : Enum<T>> Set<T>.intersect(other: EnumSet<T>): EnumSet<T> =
        EnumSet.copyOf(other).apply { retainAll(this@intersect) }

private fun Types.commonSuperType(t1: TypeMirror, t2: TypeMirror): TypeMirror? {
    if (isSubtype(t1, t2)) return t2
    if (isSubtype(t2, t1)) return t1
    if ((t1.kind.isPrimitive || t2.kind.isPrimitive) && t1.kind != t2.kind) return null
    if (t1 is DeclaredType && t2 is DeclaredType) {
        // object inheritance
        var superclass = t1
        do {
            if (isSubtype(t2, superclass)) return superclass
            superclass = (t1.asElement() as TypeElement).superclass
        } while (superclass is DeclaredType)
        // okay, no superclass. superinterface?
        val commonInterfaces = (t1.asElement() as TypeElement).allInterfaces().apply { retainAll((t2.asElement() as TypeElement).allInterfaces()) }
        if (commonInterfaces.isEmpty()) return null
        return commonInterfaces[0]
    }
    return null
}

private fun TypeElement.allInterfaces(): MutableList<TypeMirror> {
    val direct = interfaces
    val seq = direct.toMutableList()
    for (t in direct) {
       seq += ((t as DeclaredType).asElement() as TypeElement).allInterfaces()
    }
    return seq
}

private fun Elements.mirror(cls: KClass<*>): TypeMirror? = getTypeElement(cls.java.name)?.asType()

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> T.printIt(): T = this.also { Log.note { it } }
