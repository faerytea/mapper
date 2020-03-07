/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Processor.
 *
 * Mapper Processor s free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Processor s distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapper Processor  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.processor

import com.gitlab.faerytea.mapper.adapters.MappingAdapter
import com.gitlab.faerytea.mapper.adapters.Parser
import com.gitlab.faerytea.mapper.adapters.Serializer
import com.gitlab.faerytea.mapper.annotations.*
import com.gitlab.faerytea.mapper.converters.Convert
import com.gitlab.faerytea.mapper.gen.*
import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.SimpleElementVisitor8
import javax.tools.Diagnostic
import kotlin.collections.HashMap

class MappableVisitor(
        private val processor: Processor
) : SimpleElementVisitor8<HashMap<String, FieldDataBuilder>, HashMap<String, FieldDataBuilder>>() {
    private val kindOfType = EnumSet.of(ElementKind.CLASS, ElementKind.ENUM, ElementKind.ANNOTATION_TYPE, ElementKind.INTERFACE)

    inline operator fun <V> HashMap<TypeInfo, V>.get(key: TypeInfo, error: (TypeMirror) -> Exception): V = this[key] ?: throw error(key.builtBy)

    override fun defaultAction(e: Element, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> = p

    override fun visitExecutable(e: ExecutableElement, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> = e.getAnnotation(Property::class.java)?.let {
        val retType = e.returnType!!
        val name = it.value.ifEmpty { patchGetSet(e.simpleName) }
        val params: List<VariableElement> = e.parameters
        if (params.isEmpty() && !(retType.kind == TypeKind.VOID && retType is NoType)) {
            // got a getter
            val converterData = converter(e)
            val builder = p.getOrPut(name.toString(), { FieldDataBuilder(name.toString(), retType, checkRequired(e), validator(e, processor.instances)) })
            builder.getters.add(Getter(
                    builder.name,
                    e.simpleName.toString(),
                    true,
                    mapperFor(it.via, converterData?.from ?: builder.tp).second,
                    DefaultsVisitor.visit(e),
                    buildGenericsInfo(converterData?.from ?: builder.tp, e.getAnnotation(PutOnTypeArguments::class.java)?.value, e),
                    converterData,
                    checkSubtypes(retType, e)
            ))
        } else if (params.size == 1) {
            // simple setter
            val converterData = converter(e)
            val arg = params[0]
            val argType = arg.asType()
            val builder = p.getOrPut(name.toString(), { FieldDataBuilder(name.toString(), argType, checkRequired(e), validator(e, processor.instances)) })
            builder.setters.add(Setter(
                    builder.name,
                    e.simpleName.toString(),
                    Setter.Type.CLASSIC,
                    mapperFor(it.via, converterData?.from ?: builder.tp).first,
                    DefaultsVisitor.visit(e),
                    buildGenericsInfo(converterData?.from ?: builder.tp, e.getAnnotation(PutOnTypeArguments::class.java)?.value, e),
                    converterData,
                    checkSubtypes(argType, e)))
        }
        p
    } ?: run {
        // check constructor & bulk setters
        if (e.simpleName.contentEquals("<init>")) {
            // ctr
            e.getAnnotation(Mappable::class.java)?.let { _ ->
                val params = e.parameters
                class Prop(
                        val name: String,
                        val type: TypeMirror,
                        val default: String,
                        val required: Boolean,
                        val parser: AdapterInfo,
                        val converter: ConverterData?,
                        val validator: ValidatorInfo?,
                        val typeResolver: ConcreteTypeResolver?,
                        val onArgs: Array<PutOnTypeArguments.OnArg>?
                )
                val propData = params.map { prop ->
                    val converterData = converter(prop)
                    val default = DefaultsVisitor.visit(prop)
                    val propTp = prop.asType()
                    prop.getAnnotation(Property::class.java)?.run {
                        Prop(
                                this.value.ifEmpty { prop.simpleName.toString() },
                                propTp,
                                default,
                                checkRequired(prop),
                                mapperFor(via, converterData?.from ?: propTp).first,
                                converterData,
                                validator(prop, processor.instances),
                                checkSubtypes(propTp, prop),
                                prop.getAnnotation(PutOnTypeArguments::class.java)?.value
                        )
                    } ?: Prop(
                            prop.simpleName.toString(),
                            propTp,
                            default,
                            checkRequired(prop),
                            parser(converterData?.from ?: propTp),
                            converterData,
                            validator(prop, processor.instances),
                            checkSubtypes(propTp, prop),
                            prop.getAnnotation(PutOnTypeArguments::class.java)?.value
                    )
                }
                val propNames = propData.map { it.name }
                val className = (e.enclosingElement as TypeElement).qualifiedName.toString()
                for (data in propData) {
                    val builder = p.getOrPut(data.name, { FieldDataBuilder(data.name, data.type, data.required, data.validator) })
                    builder.setters += Setter(
                            propNames,
                            className,
                            Setter.Type.CONSTRUCTOR,
                            data.parser,
                            data.default,
                            buildGenericsInfo(data.converter?.from ?: data.type, data.onArgs, e),
                            data.converter,
                            data.typeResolver
                    )
                }
            }
            // no annotation -> ignore
        } else {
            val params = e.parameters
            val annotations = params.map { it.getAnnotation(Property::class.java) }
            if (annotations.all { it != null }) {
                val setterName = e.simpleName.toString()
                val names = Array(params.size) { i ->
                    annotations[i].value.ifEmpty { params[i].simpleName.toString() }
                }
                val converters = Array(params.size) { i ->
                    converter(params[i])
                }
                val defaults = Array(params.size) { i ->
                    DefaultsVisitor.visit(params[i])
                }
                val namesList = names.asList()
                for (i in params.indices) {
                    val name = names[i]
                    val param = params[i]
                    val tp = param.asType()
                    val builder = p.getOrPut(name, { FieldDataBuilder(name, tp, checkRequired(param), validator(param, processor.instances)) })
                    val converter = converters[i]
                    builder.setters += Setter(
                            namesList,
                            setterName,
                            Setter.Type.BULK,
                            mapperFor(annotations[i].via, converter?.from ?: tp).first,
                            defaults[i],
                            buildGenericsInfo(converter?.from ?: tp, param.getAnnotation(PutOnTypeArguments::class.java)?.value, param),
                            converter,
                            checkSubtypes(tp, param)
                    )
                }
            } else if (annotations.any { it != null }) {
                processor.m.printMessage(Diagnostic.Kind.WARNING, "Some parameters are annotated, but other's not; ignored.", e)
            } // else -- just a regular method, ignore
        }
        p
    }

    override fun visitVariable(e: VariableElement, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> = e.getAnnotation(Property::class.java)?.let {
        val name = it.value.ifEmpty { e.simpleName }
        val tp = e.asType()
        val converterData = converter(e)
        val default = DefaultsVisitor.visit(e)
        val (parser, serializer) = mapperFor(it.via, converterData?.from ?: tp)
        val typeResolver = checkSubtypes(tp, e)
        val onArgs = e.getAnnotation(PutOnTypeArguments::class.java)?.value
        for (mod in e.modifiers) when (mod) {
            Modifier.PRIVATE -> {
                // cannot write
                processor.m.printMessage(Diagnostic.Kind.ERROR, "Cannot use private fields: ${e.simpleName} is private", e)
            }
            Modifier.STATIC -> {
                // do not belong to object
                processor.m.printMessage(Diagnostic.Kind.ERROR, "Static members cannot be properties: ${e.simpleName} is static", e)
            }
            Modifier.FINAL -> {
                // read-only
                val builder = p.getOrPut(name.toString(), { FieldDataBuilder(name.toString(), tp, checkRequired(e), validator(e, processor.instances)) })
                builder.getters.add(Getter(
                        builder.name,
                        e.simpleName.toString(),
                        false,
                        serializer,
                        default,
                        buildGenericsInfo(converterData?.from ?: tp, onArgs, e),
                        converterData,
                        typeResolver
                ))
            }
            else -> {
                // read and write
                val builder = p.getOrPut(name.toString(), { FieldDataBuilder(name.toString(), tp, checkRequired(e), validator(e, processor.instances)) })
                builder.getters.add(Getter(
                        builder.name,
                        e.simpleName.toString(),
                        false,
                        serializer,
                        default,
                        buildGenericsInfo(converterData?.from ?: tp, onArgs, e),
                        converterData,
                        typeResolver
                ))
                builder.setters.add(Setter(
                        builder.name,
                        e.simpleName.toString(),
                        Setter.Type.DIRECT,
                        parser,
                        default,
                        buildGenericsInfo(converterData?.from ?: tp, onArgs, e),
                        converterData,
                        typeResolver
                ))
            }
        }
        p
    } ?: defaultAction(e, p)

    override fun visitType(e: TypeElement, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> =
            processor.elements.getAllMembers(e)
                    .filter { it.kind !in kindOfType } // filter out inner classes
                    .fold(p) { acc, element -> element.accept(this, acc) }

    private fun patchGetSet(name: Name): CharSequence {
        val n = name.toString()
        return if ((n.startsWith("get") || n.startsWith("set")) && (n.length > 3 && !n[3].isLowerCase())) (n[3].toLowerCase() + n.substring(4))
        else n
    }

    private fun mapperFor(specificMapper: SpecificMapper, tp: TypeMirror) = specificMapper.safeUsing().toString().run {
        if (equals(MappingAdapter::class.safeCanonicalName().toString())) {
            val parserName = specificMapper.safeUsingPar()
            val parser = if (Parser::class.safeCanonicalName().toString() == parserName.toString()) {
                parser(tp)
            } else {
                adapterInfo(parserName, specificMapper.parseUsingNamed)
            }
            val serializerName = specificMapper.safeUsingSer()
            val serializer = if (Serializer::class.safeCanonicalName().toString() == serializerName.toString()) {
                serializer(tp)
            } else {
                adapterInfo(serializerName, specificMapper.serializeUsingNamed)
            }
            parser to serializer
        } else {
            adapterInfo(this, specificMapper.usingNamed).let { it to it }
        }
    }

    private fun adapterInfo(adapterName: CharSequence, name: String) =
            AdapterInfo(adapterName, if (name.isNotEmpty()) processor.named[name].let {
                if (it == null) {
                    processor.m.printMessage(Diagnostic.Kind.ERROR, "Adapter instance ($adapterName) with name '$name' is not found")
                }
                null
            } else processor.instances[adapterName])

    private fun parser(tp: TypeMirror) = when {
        tp.kind == TypeKind.TYPEVAR && tp is TypeVariable -> AdapterInfo("var$tp")
        tp.kind == TypeKind.ARRAY && tp is ArrayType && !tp.componentType.kind.isPrimitive -> processor.parsers[TypeInfo.from(processor.objectArrayType), ::TypeNotFoundException]
        else -> processor.parsers[TypeInfo.from(tp.erase()), ::TypeNotFoundException]
    }

    private fun serializer(tp: TypeMirror) = when {
        tp.kind == TypeKind.TYPEVAR && tp is TypeVariable -> AdapterInfo("var$tp")
        tp.kind == TypeKind.ARRAY && tp is ArrayType && !tp.componentType.kind.isPrimitive -> processor.serializers[TypeInfo.from(processor.objectArrayType), ::TypeNotFoundException]
        else -> processor.serializers[TypeInfo.from(tp.erase()), ::TypeNotFoundException]
    }

    private fun buildGenericsInfo(
            tp: TypeMirror,
            onArgs: Array<PutOnTypeArguments.OnArg>?,
            element: Element
    ): List<GenericTypeInfo> = object {
        var position: Int = 0

        private fun PutOnTypeArguments.OnArg?.adapter(tp: TypeMirror) =
                if (this != null && value) mapperFor(via, tp) else parser(tp) to serializer(tp)
        private fun PutOnTypeArguments.OnArg?.converter(tp: TypeMirror) =
                if (this != null && value) ConverterVisitor.buildConverterData(processor.env, tp, (tp as DeclaredType).asElement(), convert)
                else null
        private fun PutOnTypeArguments.OnArg?.resolver(supertype: TypeMirror, element: Element) =
                if (this != null && value) checkSubtypes(supertype, element) else null

        private fun <T> Array<T>?.at(ix: Int): T? =
                if (this == null || size <= ix) null else get(ix)

        fun walk(tp: TypeMirror): List<GenericTypeInfo> = when {
            tp.kind == TypeKind.DECLARED && tp is DeclaredType -> tp.typeArguments.map {
                val onArg = onArgs.at(position)
                val converter = onArg.converter(it)
                val tpToResolve = converter?.from ?: it
                val subtypes = onArg.resolver(tpToResolve, element)
                ++position
                val genericsInfo = walk(it)
                if (subtypes != null) GenericTypeInfo(
                        it,
                        genericsInfo,
                        converter,
                        subtypes
                ) else {
                    val (par, ser) = onArg.adapter(tpToResolve)
                    GenericTypeInfo(
                            it,
                            par,
                            ser,
                            genericsInfo,
                            converter
                    )
                }
            }
            tp.kind == TypeKind.ARRAY && tp is ArrayType && !tp.componentType.kind.isPrimitive -> {
                val componentType = tp.componentType
                val onArg = onArgs.at(position)
                val converter = onArg.converter(componentType)
                val tpToResolve = converter?.from ?: componentType
                val subtypes = onArg.resolver(tpToResolve, element)
                ++position
                listOf(
                        if (subtypes != null) GenericTypeInfo(
                                componentType,
                                emptyList(),
                                converter,
                                subtypes
                        ) else {
                            val (par, ser) = onArg.adapter(tpToResolve)
                            GenericTypeInfo(
                                    componentType,
                                    par,
                                    ser,
                                    emptyList(),
                                    converter
                            )
                        }
                )
            }
            else -> {
                ++position
                emptyList()
            }
        }
    }.walk(tp)

    private fun TypeMirror.erase() = processor.types.erasure(this)

    private fun converter(e: Element) = e.accept(ConverterVisitor, processor.env)?.run {
        val name = e.getAnnotation(Convert::class.java).named
        if (name.isEmpty()) return@run this
        val instance = processor.named[name]
        if (instance == null) {
            processor.m.printMessage(Diagnostic.Kind.ERROR, "Converter ${converter.className} with name $name is not found!", e)
            return@run this
        } else {
            return@run ConverterData(AdapterInfo(converter.className, instance), from, to)
        }
    }

    private fun checkRequired(e: Element) = e.getAnnotation(Required::class.java)?.value ?: false

    internal fun SubtypeResolver.concreteTypeResolver(supertype: TypeMirror, element: Element): ConcreteTypeResolver {
        val getMapper: (ReferenceType, SpecificMapper) -> SpecifiedMapper = { refTp, annotation ->
            val (par, ser) = mapperFor(annotation, refTp)
            SpecifiedMapper(refTp, par, ser)
        }
        val resolver = ConcreteTypeResolver(
                processor.env,
                this,
                getMapper,
                getMapper(supertype as ReferenceType, onUnknown),
                buildGenericsInfo(supertype, element.getAnnotation(PutOnTypeArguments::class.java)?.value, element)
        )
        val types = processor.types
        resolver.subtypes.values.forEach {
            if (!types.isSubtype(types.erasure(it.type), types.erasure(supertype))) {
                processor.m.printMessage(Diagnostic.Kind.ERROR, "${it.type} is not subtype of $supertype", element)
            }
        }
        return resolver
    }

    private fun checkSubtypes(supertype: TypeMirror, element: Element): ConcreteTypeResolver? =
            if (supertype.kind.isPrimitive || supertype.kind == TypeKind.ARRAY) null
            else element.getAnnotation(SubtypeResolver::class.java)?.concreteTypeResolver(supertype, element)
}