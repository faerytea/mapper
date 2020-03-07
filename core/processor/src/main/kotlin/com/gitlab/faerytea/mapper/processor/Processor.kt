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

import com.gitlab.faerytea.mapper.adapters.*
import com.gitlab.faerytea.mapper.annotations.*
import com.gitlab.faerytea.mapper.gen.*
import java.io.PrintStream
import java.util.function.BiFunction
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind as L

@SupportedAnnotationTypes(
        "com.gitlab.faerytea.mapper.annotations.*"
)
@SupportedOptions(
        "mapperGeneratorName",
        "mapperDisable",
        "mapperExternalMappers",
        "mapperLogging",
        "mapperAdditionalOptions"
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class Processor : AbstractProcessor() {
    internal val elements: Elements
        get() = processingEnv.elementUtils
    internal val m: Messager
        get() = processingEnv.messager
    internal val types: Types
        get() = processingEnv.typeUtils
    /**
     * Type -> adapter info for type
     */
    internal val serializers = HashMap<TypeInfo, AdapterInfo>()
    /**
     * Type -> adapter info for type
     */
    internal val parsers = HashMap<TypeInfo, AdapterInfo>()
    /**
     * Converter -> it's info
     */
    private val converters = HashMap<TypeInfo, AdapterInfo>()
    internal val env
        get() = processingEnv
    internal val instances = HashMap<String, InstanceData>()
    internal val named = HashMap<String, InstanceData>()
    private lateinit var generator: Generator
    private var ready: Boolean = false
    private val elementVisitor = MappableVisitor(this)
    private lateinit var defaultParserType: DeclaredType
    private lateinit var defaultParserAction: ExecutableElement
    private lateinit var defaultSerializerType: DeclaredType
    private lateinit var defaultSerializerAction: ExecutableElement
    private lateinit var defaultMapperType: DeclaredType
    private lateinit var defaultParserTypePrim: Map<TypeMirror, PrimitiveType>
    private lateinit var defaultSerializerTypePrim: Map<TypeMirror, PrimitiveType>
    private lateinit var defaultArrayParserType: DeclaredType
    private lateinit var defaultArraySerializerType: DeclaredType
    private lateinit var stringType: DeclaredType
    private lateinit var stringTypeInfo: TypeInfo
    internal lateinit var objectArrayType: ArrayType

    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        try {
            Log.enabled = true
            when (val logging = processingEnvironment.options["mapperLogging"]) {
                null, "", "disable", "false" -> Log.enabled = false
                "stdout" -> Log.destination = System.out
                "stderr" -> Log.destination = System.err
                else -> Log.destination = PrintStream(logging)
            }
            Log.note { "logging enabled into ${Log.destination}" }
        } catch (e: Throwable) {
            processingEnvironment.messager.printMessage(L.WARNING, "Logging disabled due to ${e.stringTrace()}")
            Log.enabled = false
        }
        defaultParserType = elements.getTypeElement(Parser::class).asDeclaredType()
        defaultParserAction = ElementFilter.methodsIn(elements.getAllMembers(elements.getTypeElement(Parser::class.safeCanonicalName()))).last()
        defaultSerializerType = elements.getTypeElement(Serializer::class).asDeclaredType()
        defaultSerializerAction = ElementFilter.methodsIn(elements.getAllMembers(elements.getTypeElement(Serializer::class.safeCanonicalName()))).last()
        defaultMapperType = elements.getTypeElement(MappingAdapter::class).asDeclaredType()
        defaultParserTypePrim = elements.run {
            mapOf(
                    getTypeElement(ParserBoolean::class).asType() to types.getPrimitiveType(TypeKind.BOOLEAN),
                    getTypeElement(ParserInt::class).asType() to types.getPrimitiveType(TypeKind.INT),
                    getTypeElement(ParserLong::class).asType() to types.getPrimitiveType(TypeKind.LONG),
                    getTypeElement(ParserDouble::class).asType() to types.getPrimitiveType(TypeKind.DOUBLE)
            )
        }
        defaultSerializerTypePrim = elements.run {
            mapOf(
                    getTypeElement(SerializerBoolean::class).asType() to types.getPrimitiveType(TypeKind.BOOLEAN),
                    getTypeElement(SerializerInt::class).asType() to types.getPrimitiveType(TypeKind.INT),
                    getTypeElement(SerializerLong::class).asType() to types.getPrimitiveType(TypeKind.LONG),
                    getTypeElement(SerializerDouble::class).asType() to types.getPrimitiveType(TypeKind.DOUBLE)
            )
        }
        defaultArrayParserType = elements.getTypeElement(ArrayParser::class).asDeclaredType()
        defaultArraySerializerType = elements.getTypeElement(ArraySerializer::class).asDeclaredType()
        stringType = elements.getTypeElement(java.lang.String::class).asDeclaredType()
        stringTypeInfo = TypeInfo.from(stringType)
        objectArrayType = types.getArrayType(types.getDeclaredType(elements.getTypeElement("java.lang.Object")))
        val name = processingEnvironment.options["mapperGeneratorName"]
        val cls: Class<out Generator>? = try {
            val classLoader = javaClass.classLoader
            @Suppress("UNCHECKED_CAST")
            classLoader.loadClass(name) as Class<Generator>
        } catch (e: Throwable) {
            val msg = when (e) {
                is ClassCastException, is ClassNotFoundException, is LinkageError, is ExceptionInInitializerError -> "Mapper generator is disabled due to ${e.localizedMessage}"
                is NullPointerException -> "No mapping generators defined"
                else -> throw ExceptionInInitializerError(e)
            }
            m.printMessage(L.WARNING, msg)
            null
        }
        ready = processingEnvironment.options["mapperDisable"] != "true"
        if (cls != null) {
            try {
                generator = cls.getConstructor(ProcessingEnvironment::class.java).newInstance(processingEnvironment)
                generator.writePrelude()
                parsers += generator.defaultParsers
                serializers += generator.defaultSerializers
                val defaultsAnnotations = listOf(DefaultMapper::class.java, DefaultSerializer::class.java, DefaultParser::class.java)
                processingEnvironment.options["mapperExternalMappers"]?.split(',')?.forEach { entry ->
                    if (entry.isNotEmpty()) {
                        val lst = entry.split(':')
                        val externalMapperName = lst[0]
                        try {
                            val e = elements.getTypeElement(externalMapperName)!!
                            val annotation = defaultsAnnotations.find { e.getAnnotation(it) != null }
                            if (annotation == null) {
                                m.printMessage(L.WARNING, "$externalMapperName should be marked with one of ${defaultsAnnotations.map { "@${it.simpleName}" }}")
                            } else {
                                handleDefaultMappers(annotation.name, e)
                                if (lst.size >= 2) {
                                    val inst = lst[1]
                                    val lastDot = inst.lastIndexOf('.')
                                    if (lastDot == -1) {
                                        m.printMessage(L.WARNING, "Malformed instance '$inst'")
                                    } else {
                                        val holderName = inst.substring(0, lastDot)
                                        val holder = elements.getTypeElement(holderName)
                                        if (holder == null) {
                                            m.printMessage(L.WARNING, "Cannot use instance '$inst' — enclosing class $holderName not found")
                                        } else {
                                            val isMethod = inst.endsWith("()")
                                            val instName = if (isMethod) inst.substring(lastDot + 1, inst.length - 2)
                                                           else inst.substring(lastDot + 1)
                                            val found = holder.enclosedElements.find {
                                                it.simpleName.toString() == instName
                                                        && (when (it.kind) {
                                                                ElementKind.FIELD, ElementKind.ENUM_CONSTANT -> !isMethod
                                                                ElementKind.METHOD -> isMethod
                                                                else -> false
                                                            })
                                                        && it.getAnnotation(Instance::class.java) != null
                                            }
                                            if (found == null) {
                                                m.printMessage(L.WARNING, "Cannot find instance '$inst' in class ${holder.qualifiedName}")
                                            } else {
                                                handleInstance(found)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            m.printMessage(L.WARNING, "Cannot use $externalMapperName — class not found")
                        }
                    }
                }
            } catch (e: Throwable) {
                m.printMessage(L.ERROR, "Cannot create $cls: constructor of (ProcessingEnvironment) cannot be invoked;\n${e.stringTrace()}")
                ready = false
            }
        } else {
            ready = false
        }
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!ready) return false
        try {
            if (roundEnv.processingOver()) {
                generator.writeEpilogue()
                return true
            }
            var shouldGenerate = false
            for (annotation in annotations) {
                when (val annotationName = annotation.qualifiedName.toString()) {
                    DefaultMapper::class.java.name, DefaultParser::class.java.name, DefaultSerializer::class.java.name -> {
                        for (e in roundEnv.getElementsAnnotatedWith(annotation)) {
                            if (e.kind.isClass) {
                                handleDefaultMappers(annotationName, e as TypeElement)
                            } else {
                                m.printMessage(L.ERROR, "${e.simpleName} is not a class, but annotated as DefaultMapper")
                            }
                        }
                    }
                    Instance::class.java.name -> {
                        for (e in roundEnv.getElementsAnnotatedWith(annotation)) {
                            handleInstance(e)
                        }
                    }
                    Mappable::class.java.name, MappableViaSubclasses::class.java.name -> shouldGenerate = true
                }
            }
            // apply instances
            val replacer = BiFunction<TypeInfo, AdapterInfo, AdapterInfo> { _, a ->
                if (a.instance == null) {
                    instances[a.className]?.let {
                        return@BiFunction AdapterInfo(a.className, it)
                    }
                }
                return@BiFunction a
            }
            serializers.replaceAll(replacer)
            parsers.replaceAll(replacer)
            if (roundEnv.errorRaised()) return false
            m.printMessage(L.NOTE, "start mappable with:\n\tserializers: $serializers\n\tparsers: $parsers")

            return shouldGenerate then generate(
                    ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Mappable::class.java) + roundEnv.getElementsAnnotatedWith(MappableViaSubclasses::class.java)),
                    roundEnv)
        } catch (e: Exception) {
            m.printMessage(L.ERROR, "Processing aborted due to ${e.localizedMessage}\n${e.stringTrace()}")
            return false
        }
    }

    private fun handleInstance(element: Element) {
        // ensure it is public static
        if (!element.modifiers.containsAll(listOf(Modifier.PUBLIC, Modifier.STATIC))) {
            m.printMessage(L.WARNING, "instance is not public static; ignored", element)
            return
        }

        val adapterType = when (element.kind) {
            ElementKind.FIELD -> {
                element.asType()
            }
            ElementKind.ENUM_CONSTANT -> {
                element.enclosingElement.asType() // since enum constants exists only in enum declarations
            }
            ElementKind.METHOD -> {
                val method = element as ExecutableElement
                if (method.parameters.isNotEmpty()) {
                    m.printMessage(L.WARNING, "instance getter have parameters; ignored", element)
                    return
                }
                method.returnType.takeUnless { it.kind == TypeKind.VOID } ?: run {
                    m.printMessage(L.WARNING, "instance getter returns void; ignored", element)
                    return
                }
            }
            else -> {
                m.printMessage(L.WARNING, "instance is not field or method; ignored", element)
                return
            }
        }!!
        val name = element.getAnnotation(Instance::class.java).value
        val instance = InstanceData(element.enclosingElement.toString(), element.simpleName.toString(), if (name.isNotEmpty()) name else null, element.kind == ElementKind.METHOD)
        if (name.isNotEmpty()) {
            named[name] = instance
        } else {
            instances[adapterType.toString()] = instance
            converters.computeIfPresent(TypeInfo.from(adapterType)) { k, v ->
                val oldInstance = v.instance
                if (oldInstance != null && oldInstance != instance) {
                    m.printMessage(L.WARNING, "Instance for $k is already present (${oldInstance.javaAccessor()}), replacing by ${instance.javaAccessor()}")
                }
                AdapterInfo(v.className, instance)
            }
        }
    }

    private fun generate(mappables: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val generated = HashSet<String>()
        val missed = HashMap<String, String>()
        var changed: Boolean
        do {
            changed = false
            for (c in mappables) {
                val cAsType = c.asType()
                val className = cAsType.toString()
                if (className in generated) continue
                m.printMessage(L.OTHER, "got ${c.simpleName}")
                try {
                    val mappableSubclassesAnnotation = c.getAnnotation(MappableViaSubclasses::class.java)
                    val mappableAnnotation = c.getAnnotation(Mappable::class.java)
                    if (mappableAnnotation != null && mappableSubclassesAnnotation != null) {
                        m.printMessage(L.ERROR, "Both @Mappable & @MappableViaSubclasses found on ${c.simpleName}", c)
                        continue
                    }
                    val generateResult = if (mappableAnnotation != null) {
                        val onUnknown = try {
                            val handler = mappableAnnotation.onUnknown.java
                            elements.getTypeElement(handler.canonicalName)
                        } catch (e: MirroredTypeException) {
                            types.asElement(e.typeMirror) as TypeElement
                        }!!
                        if (onUnknown.methods().find { e ->
                                    Log.note { e }
                                    Log.note { e.simpleName.toString() == "handle" }
                                    Log.note { e.parameters.size == 2 }
                                    Log.note { e.returnType.kind == TypeKind.VOID }
                                    Log.note { types.isSameType(e.parameters[0].asType(), elements.getTypeElement("java.lang.String").asType()) }
                                    Log.note { types.isAssignable(generator.inputTypeName, e.parameters[1].asType()) }
                                    e.simpleName.toString() == "handle"
                                            && e.parameters.size == 2
                                            && e.returnType.kind == TypeKind.VOID
                                            && types.isSameType(e.parameters[0].asType(), elements.getTypeElement("java.lang.String").asType())
                                            && types.isAssignable(generator.inputTypeName, e.parameters[1].asType())
                                } == null) {
                            m.printMessage(L.ERROR, "$onUnknown is not compatible", c)
                            return false
                        }
                        val onUnknownInstance = mappableAnnotation.onUnknownNamed.run {
                            if (isEmpty()) instances[this] else named[this]
                        }
                        val onUnknownAdapterInfo = AdapterInfo(onUnknown.qualifiedName.toString(), onUnknownInstance)
                        if (c.kind == ElementKind.ENUM) {
                            val stringParser = parsers[stringTypeInfo]
                            val stringSerializer = serializers[stringTypeInfo]
                            if (stringParser == null || stringSerializer == null)
                                throw TypeNotFoundException(stringType)
                            val constants = c.enclosedElements
                                    .filter { it.kind == ElementKind.ENUM_CONSTANT }
                                    .associate { field ->
                                        val name = field.getAnnotation(Property::class.java)?.value
                                        val fieldName = field.simpleName.toString()
                                        val actualName = if (name.isNullOrEmpty()) fieldName else name
                                        actualName to fieldName
                                    }
                            generator.generateFor(c, constants, onUnknownAdapterInfo, stringParser, stringSerializer)
                        } else {
                            val mappings = c.accept(elementVisitor, HashMap())
                                    .mapValues { (_, v) -> FieldData(v.name, v.tp, v.getters, v.setters, v.required, v.validator) }
                            if (roundEnv.errorRaised()) return false
                            generator.generateFor(c, mappings, onUnknownAdapterInfo, validator(c, instances))
                        }
                    } else {
                        assert(mappableSubclassesAnnotation != null)
                        generator.generateFor(
                                c,
                                elementVisitor.run { mappableSubclassesAnnotation.value.concreteTypeResolver(c.asType(), c) },
                                mappableSubclassesAnnotation.markAsDefault,
                                null
                        )
                    }
                    if (generateResult.canParse)
                        parsers[TypeInfo.from(types.erasure(cAsType))] = generateResult.adapter
                    if (generateResult.canSerialize)
                        serializers[TypeInfo.from(types.erasure(cAsType))] = generateResult.adapter
                    generated.add(className)
                    changed = true
                    m.printMessage(L.OTHER, "$generateResult was generated for $className")
                } catch (e: TypeNotFoundException) {
                    val tp = e.key

                    missed[className] = tp.toString()
                    m.printMessage(L.OTHER, "$className is missing for $tp")
                } catch (e: Exception) {
                    m.printMessage(L.ERROR, "Generator $generator failed to generate adapter for ${c.qualifiedName}: ${e.stringTrace()}", c)
                }
            }
            missed -= generated
            // break circulars
            if (missed.isNotEmpty()) {
                m.printMessage(L.OTHER, "breaking circulars for $missed")
                val pool = HashSet<String>(missed.keys)
                val stack = ArrayList<String>(missed.size - 1)
                while (pool.isNotEmpty()) {
                    stack.add(pool.first())
                    while (stack.isNotEmpty()) {
                        val key = stack.last()
                        val value = missed[key]
                        if (value !in pool) {
                            pool.removeAll(stack)
                            stack.clear()
                        } else {
                            value!!
                            val index = stack.indexOf(value)
                            if (index == -1) {
                                stack.add(value)
                            } else {
                                // found circular
                                val circle = stack.subList(index, stack.size)
                                m.printMessage(L.WARNING, "Found circular dependency. Generated classes may be broken. Classes: $circle")
                                for (name in circle) {
                                    val typeElement = elements.getTypeElement(name)
                                    val type = types.erasure(typeElement.asType())
                                    val generateResult = generator.nameFor(typeElement)
                                    if (generateResult.canParse)
                                        parsers[TypeInfo.from(type)] = generateResult.adapter
                                    if (generateResult.canSerialize)
                                        serializers[TypeInfo.from(type)] = generateResult.adapter
                                    m.printMessage(L.OTHER, "fake $generateResult generated for $name")
                                }
                                generator.notifyCircularDependency(circle.map { elements.getTypeElement(it) })
                                pool.removeAll(circle)
                                circle.clear()
                            }
                        }
                    }
                }
            }
        } while (changed)
        if (generated.isEmpty() && missed.isNotEmpty())
            m.printMessage(L.ERROR, "Generator $generator failed to generate at least one mapper (mappers for ${missed.keys} aren't generated)")
        return missed.isEmpty()
    }


    private fun handleDefaultMappers(annotationName: String, mapper: TypeElement) {
        val isParser = annotationName != DefaultSerializer::class.java.toString()
        val isSerializer = annotationName != DefaultParser::class.java.toString()
        assert(isParser || isSerializer)
        val mapperAsDeclaredType = types.getDeclaredType(mapper)
        val isObjectParser = types.isAssignable(mapperAsDeclaredType, types.erasure(defaultParserType))
        val isObjectSerializer = types.isAssignable(mapperAsDeclaredType, types.erasure(defaultSerializerType))
        if (isObjectParser || isObjectSerializer) {
            if (isParser && isObjectParser) {
                val actuallyAType = (types.asMemberOf(mapperAsDeclaredType, defaultParserAction) as ExecutableType).returnType
                parsers.put(TypeInfo.from(types.erasure(actuallyAType)), AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                    m.printMessage(L.WARNING, "Mapper for $actuallyAType has many default mappers! It was $it, now replaced by ${mapper.qualifiedName}.")
                }
            }
            if (isSerializer && isObjectSerializer) {
                val actuallyAType = (types.asMemberOf(mapperAsDeclaredType, defaultSerializerAction) as ExecutableType).parameterTypes[0]
                serializers.put(TypeInfo.from(types.erasure(actuallyAType)), AdapterInfo(mapper.qualifiedName))?.let {
                    m.printMessage(L.WARNING, "Mapper for $actuallyAType has many default mappers! It was ${it.className}, now replaced by ${mapper.qualifiedName}.")
                }
            }
        } else {
            var found = false
            for ((i, tp) in defaultSerializerTypePrim) {
                if (types.isAssignable(mapperAsDeclaredType, types.erasure(i))) {
                    serializers.put(TypeInfo.from(types.erasure(tp)), AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                        m.printMessage(L.WARNING, "Mapper for $tp has many default mappers! It was ${it.className}, now replaced by ${mapper.qualifiedName}.")
                    }
                    found = true
                }
            }
            for ((i, tp) in defaultParserTypePrim) {
                if (types.isAssignable(mapperAsDeclaredType, types.erasure(i))) {
                    parsers.put(TypeInfo.from(types.erasure(tp)), AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                        m.printMessage(L.WARNING, "Mapper for $tp has many default mappers! It was ${it.className}, now replaced by ${mapper.qualifiedName}.")
                    }
                    found = true
                }
            }
            if (!found) {
                // generic arrays (primitive arrays should implement MappingAdapter<x[]>
                if (types.isAssignable(mapperAsDeclaredType, defaultArrayParserType))
                    parsers[TypeInfo.from(objectArrayType)] =
                            AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()])
                if (types.isAssignable(mapperAsDeclaredType, defaultArraySerializerType))
                    serializers[TypeInfo.from(objectArrayType)] =
                            AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()])
            }
            if (!found) {
                // maybe it is generic mapper?
                val methods = mapper.methods()
                val toObject = ArrayList<ExecutableElement>(1)
                val write = ArrayList<ExecutableElement>(1)
                val applySer = ArrayList<ExecutableElement>(1)
                val applyPar = ArrayList<ExecutableElement>(1)
                val inputTp = generator.inputTypeName
                val outputTp = generator.outputTypeName
                // so
                // Tp parsers:
                // <A, B, C> Tp<A, B, C> toObject(Input, Parser<A>, Parser<B>, Parser<C>)
                for (method in methods) {
                    if (method.parameters.isEmpty()) continue
                    val parameters = method.parameters
                    if (method.typeParameters.isEmpty()) continue
                    val returnType = method.returnType
                    val typeParameters = method.typeParameters
                    val firstArgTp = parameters[0].asType()
                    if (method.simpleName.toString() == "toObject"
                            && parameters.size > 1
                            && types.isAssignable(generator.inputTypeName, firstArgTp)
                            && returnType is DeclaredType
                            && returnType.typeArguments.count { it.kind == TypeKind.TYPEVAR } == parameters.size - 1
                            && parameters.size - 1 == typeParameters.size
                            && run {
                                var correct = true
                                val retTpTA = returnType.typeArguments.filter { it.kind == TypeKind.TYPEVAR }
                                for (i in typeParameters.indices) {
                                    val retTpArg = retTpTA[i]
                                    val typeParameter = typeParameters[i].asType()
                                    val parserTp = parameters[i + 1].asType()
                                    if (!(parserTp is DeclaredType
                                                    && types.isSameType(types.erasure(parserTp), types.erasure(defaultParserType))
                                                    && parserTp.typeArguments.size == 2
                                                    && parserTp.typeArguments[0] == retTpArg
                                                    && parserTp.typeArguments[1] == inputTp
                                                    && retTpArg == typeParameter)) {
                                        correct = false
                                        break
                                    }
                                }
                                correct
                            }) {
                        // actually, correct "toObject" signature
                        toObject.add(method)
                    }
                    if (method.simpleName.toString() == "write"
                            && parameters.size > 2
                            && returnType.kind == TypeKind.VOID
                            && types.isAssignable(outputTp, parameters[1].asType())
                            && firstArgTp is DeclaredType
                            && parameters.size - 2 == typeParameters.size
                            && run {
                                var correct = true
                                for (i in typeParameters.indices) {
                                    val typeParameter = typeParameters[i].asType()
                                    val serializerTp = parameters[i + 2].asType()
                                    if (!(serializerTp is DeclaredType
                                                    && types.isSameType(types.erasure(serializerTp), types.erasure(defaultSerializerType))
                                                    && serializerTp.typeArguments.size == 2
                                                    && types.isSameType(serializerTp.typeArguments[0], typeParameter)
                                                    && types.isSameType(serializerTp.typeArguments[1], outputTp))) {
                                        correct = false
                                        break
                                    }
                                }
                                correct
                            }) {
                        // actually, correct "write" signature
                        write.add(method)
                    }
                    if (method.simpleName.toString() == "apply"
                            && parameters.size == typeParameters.size
                            && returnType is DeclaredType
                            && returnType.typeArguments.isNotEmpty()
                            && run {
                                var correct = true
                                for (i in typeParameters.indices) {
                                    val typeParameter = typeParameters[i].asType()
                                    val mapperTp = parameters[i].asType()
                                    if (!(mapperTp is DeclaredType
                                                    && mapperTp.typeArguments.isNotEmpty()
                                                    && mapperTp.typeArguments[0] == typeParameter)) {
                                        correct = false
                                        break
                                    }
                                }
                                correct
                            }) {
                        // actually, almost correct "apply" signature
                        val serErased = types.erasure(defaultSerializerType)
                        val parErased = types.erasure(defaultParserType)
                        val mapErased = types.erasure(defaultMapperType)
                        val retErased = types.erasure(returnType)
                        when {
                            parameters.all { types.isSameType(types.erasure(it.asType()), serErased) }
                                    && types.isSameType(retErased, serErased) -> applySer.add(method)
                            parameters.all { types.isSameType(types.erasure(it.asType()), parErased) }
                                    && types.isSameType(retErased, parErased) -> applyPar.add(method)
                            parameters.all { types.isSameType(types.erasure(it.asType()), mapErased) }
                                    && types.isSameType(retErased, mapErased) -> { /* ok boomer */ }
                            else -> m.printMessage(L.WARNING, "found apply() with weird signature; ignored", method)
                        }
                    }
                }
                for (app in applyPar) {
                    val erasure = types.erasure((app.returnType as DeclaredType).typeArguments[0])
                    val erasedTypeInfo = TypeInfo.from(erasure)
                    for (toObj in toObject) {
                        if (types.isSameType(types.erasure(toObj.returnType), erasure)) {
                            parsers.put(erasedTypeInfo, AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                                m.printMessage(L.WARNING, "Mapper for $erasure has many default mappers! It was $it, now replaced by ${mapper.qualifiedName}.")
                            }
                            found = true
                        }
                    }
                }
                for (app in applySer) {
                    val erasure = types.erasure((app.returnType as DeclaredType).typeArguments[0])
                    val erasedTypeInfo = TypeInfo.from(erasure)
                    for (w in write) {
                        if (types.isSameType(types.erasure(w.parameters[0].asType() as DeclaredType), erasure)) {
                            serializers.put(erasedTypeInfo, AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                                m.printMessage(L.WARNING, "Mapper for $erasure has many default mappers! It was $it, now replaced by ${mapper.qualifiedName}.")
                            }
                            found = true
                        }
                    }
                }

            }
            if (!found) m.printMessage(L.ERROR, "${mapper.qualifiedName} annotated as DefaultMapper, but is not an MappingAdapter")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline infix fun Boolean.then(x: Boolean): Boolean = !this || (this && x)
}