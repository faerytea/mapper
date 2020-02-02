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
import com.gitlab.faerytea.mapper.annotations.Mappable
import com.gitlab.faerytea.mapper.annotations.Property
import com.gitlab.faerytea.mapper.annotations.Required
import com.gitlab.faerytea.mapper.converters.Convert
import com.gitlab.faerytea.mapper.gen.*
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.SimpleElementVisitor8
import javax.tools.Diagnostic

class MappableVisitor(
        private val processor: Processor
) : SimpleElementVisitor8<HashMap<String, FieldDataBuilder>, HashMap<String, FieldDataBuilder>>() {
    inline operator fun <V> HashMap<TypeMirror, V>.get(key: TypeMirror, error: (TypeMirror) -> Exception): V = this[key] ?: throw error(key)

    override fun defaultAction(e: Element, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> = p

    override fun visitExecutable(e: ExecutableElement, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> = e.getAnnotation(Property::class.java)?.let {
        processor.printWriter.appendln("Visiting e: $e, p is $p")
        processor.printWriter.flush()
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
                    mapperFor(it, converterData?.from ?: builder.tp).second,
                    DefaultsVisitor.visit(e),
                    buildGenericsInfo(converterData?.from ?: builder.tp, false, ::serializer),
                    converterData
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
                    mapperFor(it, converterData?.from ?: builder.tp).first,
                    DefaultsVisitor.visit(e),
                    buildGenericsInfo(converterData?.from ?: builder.tp, true, ::parser),
                    converterData))
        }
        p
    } ?: run {
        // check constructor & bulk setters
        if (e.simpleName.contentEquals("<init>")) {
            processor.printWriter.appendln("found ctor")
            processor.printWriter.flush()
            // ctr
            e.getAnnotation(Mappable::class.java)?.let { _ ->
                processor.printWriter.appendln("with annotation")
                processor.printWriter.flush()
                val params = e.parameters
                processor.printWriter.appendln("params $params")
                processor.printWriter.flush()
                data class Prop(
                        val name: String,
                        val type: TypeMirror,
                        val default: String,
                        val required: Boolean,
                        val parser: AdapterInfo,
                        val converter: ConverterData?,
                        val validator: ValidatorInfo?
                )
                val propData = params.map { prop ->
                    val converterData = converter(prop)
                    val default = DefaultsVisitor.visit(prop)
                    val propTp = prop.asType()
                    prop.getAnnotation(Property::class.java)?.run {
                        Prop(this.value.ifEmpty { prop.simpleName.toString() }, propTp, default, checkRequired(prop), mapperFor(this, converterData?.from ?: propTp).first, converterData, validator(prop, processor.instances))
                    } ?: Prop(prop.simpleName.toString(), propTp, default, checkRequired(prop), parser(converterData?.from ?: propTp), converterData, validator(prop, processor.instances))
                }
                processor.printWriter.appendln("propData: $propData")
                processor.printWriter.flush()
                val propNames = propData.map { it.name }
                processor.printWriter.appendln("propNames: $propNames")
                processor.printWriter.flush()
                val className = (e.enclosingElement as TypeElement).qualifiedName.toString()
                processor.printWriter.appendln("class name: $className")
                processor.printWriter.flush()
                for (data in propData) {
                    val builder = p.getOrPut(data.name, { FieldDataBuilder(data.name, data.type, data.required, data.validator) })
                    builder.setters += Setter(
                            propNames,
                            className,
                            Setter.Type.CONSTRUCTOR,
                            data.parser,
                            data.default,
                            buildGenericsInfo(data.converter?.from ?: data.type, true, ::parser),
                            data.converter
                    )
                    processor.printWriter.appendln("f ${data.name}: $builder")
                    processor.printWriter.flush()
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
                    val tp = params[i].asType()
                    val builder = p.getOrPut(name, { FieldDataBuilder(name, tp, checkRequired(params[i]), validator(params[i], processor.instances)) })
                    val converter = converters[i]
                    builder.setters += Setter(
                            namesList,
                            setterName,
                            Setter.Type.BULK,
                            mapperFor(annotations[i], converter?.from ?: tp).first,
                            defaults[i],
                            buildGenericsInfo(converter?.from ?: tp, true, ::parser),
                            converter
                    )
                }
            } else if (annotations.any { it != null }) {
                processor.m.printMessage(Diagnostic.Kind.WARNING, "Some parameters are annotated, but other's not; ignored.", e)
            } // else -- just a regular method, ignore
        }
        p
    }

    override fun visitVariable(e: VariableElement, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder> = e.getAnnotation(Property::class.java)?.let {
        processor.printWriter.appendln("Visiting v: $e, p is $p")
        processor.printWriter.flush()
        val name = it.value.ifEmpty { e.simpleName }
        processor.printWriter.appendln("name: $name")
        processor.printWriter.flush()
        val tp = e.asType()
        processor.printWriter.appendln("tp: $tp")
        processor.printWriter.flush()
        val converterData = converter(e)
        val default = DefaultsVisitor.visit(e)
        val (parser, serializer) = mapperFor(it, converterData?.from ?: tp)
        processor.printWriter.appendln("mappers: $parser, $serializer")
        processor.printWriter.flush()
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
                        buildGenericsInfo(converterData?.from ?: tp, false, ::serializer),
                        converterData
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
                        buildGenericsInfo(converterData?.from ?: tp, true, ::parser),
                        converterData
                ))
                builder.setters.add(Setter(
                        builder.name,
                        e.simpleName.toString(),
                        Setter.Type.DIRECT,
                        parser,
                        default,
                        buildGenericsInfo(converterData?.from ?: tp, false, ::serializer),
                        converterData
                ))
            }
        }
        p
    } ?: defaultAction(e, p)

    override fun visitType(e: TypeElement, p: HashMap<String, FieldDataBuilder>): HashMap<String, FieldDataBuilder>
            = processor.elements.getAllMembers(e).fold(p) { acc, element -> element.accept(this, acc) }

    private fun patchGetSet(name: Name): CharSequence {
        val n = name.toString()
        return if (n.startsWith("get") || n.startsWith("set")) (n[3].toLowerCase() + n.substring(4))
        else n
    }

    private fun mapperFor(property: Property, tp: TypeMirror) = property.safeUsing().toString().run {
        processor.printWriter.appendln("mapper for $tp: using $this")
        processor.printWriter.flush()
        if (equals(MappingAdapter::class.safeCanonicalName().toString())) {
            val parserName = property.safeUsingPar()
            val parser = if (Parser::class.safeCanonicalName().toString() == parserName.toString()) {
                parser(tp)
            } else {
                adapterInfo(parserName, property.parseUsingNamed)
            }
            val serializerName = property.safeUsingSer()
            val serializer = if (Serializer::class.safeCanonicalName().toString() == serializerName.toString()) {
                serializer(tp)
            } else {
                adapterInfo(serializerName, property.serializeUsingNamed)
            }
            parser to serializer
        } else {
            adapterInfo(this, property.usingNamed).let { it to it }
        }
    }.also { processor.printWriter.flush() }

    private fun adapterInfo(adapterName: CharSequence, name: String) =
            AdapterInfo(adapterName, if (name.isNotEmpty()) processor.named[name].let {
                if (it == null) {
                    processor.m.printMessage(Diagnostic.Kind.ERROR, "Adapter instance ($adapterName) with name '$name' is not found")
                }
                null
            } else processor.instances[adapterName])

    private fun parser(tp: TypeMirror) = when {
        tp.kind == TypeKind.TYPEVAR && tp is TypeVariable -> AdapterInfo("var$tp")
        tp.kind == TypeKind.ARRAY && tp is ArrayType && !tp.componentType.kind.isPrimitive -> processor.parsers[processor.objectArrayType, ::TypeNotFoundException]
        else -> processor.parsers[tp.erase(), ::TypeNotFoundException]
    }

    private fun serializer(tp: TypeMirror) = when {
        tp.kind == TypeKind.TYPEVAR && tp is TypeVariable -> AdapterInfo("var$tp")
        tp.kind == TypeKind.ARRAY && tp is ArrayType && !tp.componentType.kind.isPrimitive -> processor.serializers[processor.objectArrayType, ::TypeNotFoundException]
        else -> processor.serializers[tp.erase(), ::TypeNotFoundException]
    }

    private fun buildGenericsInfo(
            tp: TypeMirror,
            isParser: Boolean,
            adapters: (TypeMirror) -> AdapterInfo = if (isParser) ::parser else ::serializer
    ): List<GenericTypeInfo> {
        return when {
            tp.kind == TypeKind.DECLARED && tp is DeclaredType -> tp.typeArguments.map {
                val canMap = runCatching { parser(it).className == serializer(it).className }.getOrElse { false }
                val genericsInfo = buildGenericsInfo(it, isParser, adapters)
                GenericTypeInfo(it, adapters(it), genericsInfo, when {
                    canMap -> GenericTypeInfo.AdapterType.MAPPER
                    isParser -> GenericTypeInfo.AdapterType.PARSER
                    else -> GenericTypeInfo.AdapterType.SERIALIZER
                })
            }
            tp.kind == TypeKind.ARRAY && tp is ArrayType && !tp.componentType.kind.isPrimitive -> {
                listOf(GenericTypeInfo(tp.componentType, adapters(tp.componentType), emptyList(), when {
                    runCatching { parser(tp.componentType).className == serializer(tp.componentType).className }.getOrElse { false } -> GenericTypeInfo.AdapterType.MAPPER
                    isParser -> GenericTypeInfo.AdapterType.PARSER
                    else -> GenericTypeInfo.AdapterType.SERIALIZER
                }))
            }
            else -> emptyList()
        }
    }

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
}