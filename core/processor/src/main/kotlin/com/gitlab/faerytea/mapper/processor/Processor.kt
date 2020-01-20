package com.gitlab.faerytea.mapper.processor

import com.gitlab.faerytea.mapper.adapters.*
import com.gitlab.faerytea.mapper.annotations.*
import com.gitlab.faerytea.mapper.gen.*
import java.io.File
import java.io.PrintWriter
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
@SupportedOptions("mapperGeneratorName")
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
    internal val serializers = HashMap<TypeMirror, AdapterInfo>()
    /**
     * Type -> adapter info for type
     */
    internal val parsers = HashMap<TypeMirror, AdapterInfo>()
    /**
     * Converter -> it's info
     */
    private val converters = HashMap<TypeMirror, AdapterInfo>()
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
    internal lateinit var objectArrayType: ArrayType
//    private lateinit var defaultCollectionParserType: DeclaredType
//    private lateinit var defaultCollectionSerializerType: DeclaredType

    internal val printWriter = PrintWriter(File("/tmp/test.txt"))

    override fun init(processingEnvironment: ProcessingEnvironment) {
        printWriter.println("init called")
        printWriter.flush()
        super.init(processingEnvironment)
        defaultParserType = elements.getTypeElement(Parser::class.safeCanonicalName()).asType() as DeclaredType
        defaultParserAction = ElementFilter.methodsIn(elements.getAllMembers(elements.getTypeElement(Parser::class.safeCanonicalName()))).last()
        defaultSerializerType = elements.getTypeElement(Serializer::class.safeCanonicalName()).asType() as DeclaredType
        defaultSerializerAction = ElementFilter.methodsIn(elements.getAllMembers(elements.getTypeElement(Serializer::class.safeCanonicalName()))).last()
        defaultMapperType = elements.getTypeElement(MappingAdapter::class.safeCanonicalName()).asType() as DeclaredType
        defaultParserTypePrim = elements.run {
            mapOf(
                    getTypeElement(ParserInt::class.safeCanonicalName()).asType() to types.getPrimitiveType(TypeKind.INT),
                    getTypeElement(ParserDouble::class.safeCanonicalName()).asType() to types.getPrimitiveType(TypeKind.DOUBLE)
            )
        }
        defaultSerializerTypePrim = elements.run {
            mapOf(
                    getTypeElement(SerializerInt::class.safeCanonicalName()).asType() to types.getPrimitiveType(TypeKind.INT),
                    getTypeElement(SerializerDouble::class.safeCanonicalName()).asType() to types.getPrimitiveType(TypeKind.DOUBLE)
            )
        }
        defaultArrayParserType = elements.getTypeElement(ArrayParser::class.safeCanonicalName()).asType() as DeclaredType
        defaultArraySerializerType = elements.getTypeElement(ArraySerializer::class.safeCanonicalName()).asType() as DeclaredType
        objectArrayType = types.getArrayType(types.getDeclaredType(elements.getTypeElement("java.lang.Object")))
//        defaultCollectionParserType = elements.getTypeElement(CollectionsParser::class.safeCanonicalName()).asType() as DeclaredType
//        defaultCollectionSerializerType = elements.getTypeElement(CollectionsSerializer::class.safeCanonicalName()).asType() as DeclaredType
        printWriter.appendln("def par meth ${ElementFilter.methodsIn(elements.getTypeElement(Parser::class.safeCanonicalName()).enclosedElements)}")
        printWriter.appendln("def ser meth ${ElementFilter.methodsIn(elements.getTypeElement(Serializer::class.safeCanonicalName()).enclosedElements)}")
        printWriter.appendln("def parse: $defaultParserType, def ser: $defaultSerializerType")
        val name = processingEnvironment.options["mapperGeneratorName"]
        val cls: Class<out Generator>? = try {
            val classLoader = javaClass.classLoader
            @Suppress("UNCHECKED_CAST")
            classLoader.loadClass(name) as Class<Generator>
        } catch (e: Throwable) {
            e.printStackTrace(printWriter)
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
            } catch (e: Throwable) {
                m.printMessage(L.ERROR, "Cannot create $cls: constructor of (ProcessingEnvironment) cannot be invoked;\n${e.stringTrace()}")
                ready = false
            }
        } else {
            ready = false
        }
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        printWriter.appendln("process called")
        printWriter.flush()
        if (!ready) return false
        if (roundEnv.processingOver()) {
            generator.writeEpilogue()
            return true
        }
        printWriter.appendln("start")
        var shouldGenerate = false
        for (annotation in annotations) {
            printWriter.appendln("got ${annotation.qualifiedName}")
            when (val annotationName = annotation.qualifiedName.toString()) {
                DefaultMapper::class.java.name, DefaultParser::class.java.name, DefaultSerializer::class.java.name -> {
                    for (e in roundEnv.getElementsAnnotatedWith(annotation)) {
                        printWriter.appendln("processing $e (${e.simpleName})")
                        if (e.kind.isClass) {
                            handleDefaultMappers(annotationName, e as TypeElement)
                        } else {
                            m.printMessage(L.ERROR, "${e.simpleName} is not a class, but annotated as DefaultMapper")
                        }
                        printWriter.flush()
                    }
                }
                Instance::class.java.name -> {
                    for (e in roundEnv.getElementsAnnotatedWith(annotation)) {
                        handleInstance(e)
                    }
                }
                Mappable::class.java.name -> shouldGenerate = true
            }
        }
        // apply instances
        val replacer = BiFunction<TypeMirror, AdapterInfo, AdapterInfo> { _, a ->
            if (a.instance == null) {
                instances[a.className]?.let {
                    return@BiFunction AdapterInfo(a.className, it)
                }
            }
            return@BiFunction a
        }
        serializers.replaceAll(replacer)
        parsers.replaceAll(replacer)
        printWriter.appendln("start mappable with:")
        printWriter.appendln("\tserializers: $serializers")
        printWriter.appendln("\tparsers: $parsers")
        printWriter.flush()

        return shouldGenerate then generate(ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Mappable::class.java)))
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
            converters.computeIfPresent(adapterType) { k, v ->
                val oldInstance = v.instance
                if (oldInstance != null && oldInstance != instance) {
                    m.printMessage(L.WARNING, "Instance for $k is already present (${oldInstance.javaAccessor()}), replacing by ${instance.javaAccessor()}")
                }
                AdapterInfo(v.className, instance)
            }
        }
    }

    private fun generate(mappables: Set<TypeElement>): Boolean {
        val generated = HashSet<String>()
        val missed = HashMap<String, String>()
        var changed: Boolean
        do {
            printWriter.appendln("start round mappable with:")
            printWriter.appendln("\tserializers: $serializers")
            printWriter.appendln("\tparsers: $parsers")
            printWriter.flush()
            changed = false
            for (c in mappables) {
                val cAsType = c.asType()
                val className = cAsType.toString()
                if (className in generated) continue
                printWriter.appendln("got ${c.simpleName}")
                try {
                    val mappableAnnotation = c.getAnnotation(Mappable::class.java)!!
                    val onUnknown = try {
                        val handler = mappableAnnotation.onUnknown.java
                        elements.getTypeElement(handler.canonicalName)
                    } catch (e: MirroredTypeException) {
                        types.asElement(e.typeMirror) as TypeElement
                    }!!
                    if (onUnknown.methods().find { e ->
                                println(e)
                                println(e.simpleName.toString() == "handle")
                                println(e.parameters.size == 2)
                                println(e.returnType.kind == TypeKind.VOID)
                                println(types.isSameType(e.parameters[0].asType(), elements.getTypeElement("java.lang.String").asType()))
                                println(types.isAssignable(generator.inputTypeName, e.parameters[1].asType()))
                                e.simpleName.toString() == "handle"
                                        && e.parameters.size == 2
                                        && e.returnType.kind == TypeKind.VOID
                                        && types.isSameType(e.parameters[0].asType(), elements.getTypeElement("java.lang.String").asType())
                                        && types.isAssignable(generator.inputTypeName, e.parameters[1].asType())
                            } == null) {
                        m.printMessage(L.ERROR, "$onUnknown is not compatible", c)
                        return false
                    }
                    val onUnknownInstance = mappableAnnotation.onUnknownNamed.toString().run {
                        if (isEmpty()) instances[this] else named[this]
                    }
                    val mappings = c.accept(elementVisitor, HashMap())
                            .mapValues { (_, v) -> FieldData(v.name, v.tp, v.getters, v.setters, v.required, v.validator) }
                    val generateResult = generator.generateFor(c, mappings, AdapterInfo(onUnknown.qualifiedName.toString(), onUnknownInstance), validator(c, instances))
                    if (generateResult.canParse)
                        parsers[types.erasure(cAsType)] = generateResult.adapter
                    if (generateResult.canSerialize)
                        serializers[types.erasure(cAsType)] = generateResult.adapter
                    generated.add(className)
                    changed = true
                    printWriter.appendln("$generateResult was generated for $className")
                } catch (e: TypeNotFoundException) {
                    missed[className] = e.key.toString()
                    printWriter.appendln("$className is missing for ${e.key}")
                } catch (e: Exception) {
                    m.printMessage(L.ERROR, "Generator $generator failed to generate adapter for ${c.qualifiedName}: ${e.stringTrace()}", c)
                    printWriter.appendln("${e.stringTrace()} caught!")
                }
            }
            printWriter.flush()
            missed -= generated
            // break circulars
            if (missed.isNotEmpty()) {
                printWriter.appendln("breaking circulars for $missed")
                val pool = HashSet<String>(missed.keys)
                val stack = ArrayList<String>(missed.size - 1)
                while (pool.isNotEmpty()) {
                    printWriter.appendln("current pool is $pool")
                    stack.add(pool.first())
                    while (stack.isNotEmpty()) {
                        printWriter.appendln("current stack is $stack")
                        val key = stack.last()
                        val value = missed[key]
                        printWriter.appendln("got $key -> $value")
                        if (value !in pool) {
                            pool.removeAll(stack)
                            stack.clear()
                        } else {
                            value!!
                            val index = stack.indexOf(value)
                            if (index == -1) {
                                printWriter.appendln("$value not found in stack")
                                stack.add(value)
                            } else {
                                // found circular
                                val circle = stack.subList(index, stack.size)
                                printWriter.appendln("$value is found in stack, circle is $circle")
                                m.printMessage(L.WARNING, "Found circular dependency. Generated classes may be broken. Classes: $circle")
                                for (name in circle) {
                                    val typeElement = elements.getTypeElement(name)
                                    val type = types.erasure(typeElement.asType())
                                    val generateResult = generator.nameFor(typeElement)
                                    if (generateResult.canParse)
                                        parsers[type] = generateResult.adapter
                                    if (generateResult.canSerialize)
                                        serializers[type] = generateResult.adapter
                                    printWriter.appendln("fake $generateResult generated for $name")
                                }
                                generator.notifyCircularDependency(circle.map { elements.getTypeElement(it) });
                                printWriter.flush()
                                pool.removeAll(circle)
                                circle.clear()
                            }
                        }
                    }
                }
            }
            printWriter.flush()
        } while (changed)
        if (generated.isEmpty() && missed.isNotEmpty())
            m.printMessage(L.ERROR, "Generator $generator failed to generate at least one mapper (mappers for ${missed.keys} aren't generated)")
        printWriter.appendln("done.")
        printWriter.flush()
        return missed.isEmpty()
    }


    private fun handleDefaultMappers(annotationName: String, mapper: TypeElement) {
        val isParser = annotationName != DefaultSerializer::class.java.toString()
        val isSerializer = annotationName != DefaultParser::class.java.toString()
        printWriter.appendln("isParser: $isParser; isSerializer: $isSerializer")
        assert(isParser || isSerializer)
        val mapperAsDeclaredType = types.getDeclaredType(mapper)
        printWriter.appendln("$mapper, $mapperAsDeclaredType")
        val isObjectParser = types.isAssignable(mapperAsDeclaredType, types.erasure(defaultParserType))
        val isObjectSerializer = types.isAssignable(mapperAsDeclaredType, types.erasure(defaultSerializerType))
        printWriter.appendln("isObjectParser: $isObjectParser; isObjectSerializer: $isObjectSerializer")
        if (isObjectParser || isObjectSerializer) {
            if (isParser && isObjectParser) {
                printWriter.flush()
                val actuallyAType = (types.asMemberOf(mapperAsDeclaredType, defaultParserAction) as ExecutableType).returnType
                parsers.put(types.erasure(actuallyAType), AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                    m.printMessage(L.WARNING, "Mapper for $actuallyAType has many default mappers! It was $it, now replaced by ${mapper.qualifiedName}.")
                }
            }
            if (isSerializer && isObjectSerializer) {
                val actuallyAType = (types.asMemberOf(mapperAsDeclaredType, defaultSerializerAction) as ExecutableType).parameterTypes[0]
                serializers.put(types.erasure(actuallyAType), AdapterInfo(mapper.qualifiedName))?.let {
                    m.printMessage(L.WARNING, "Mapper for $actuallyAType has many default mappers! It was ${it.className}, now replaced by ${mapper.qualifiedName}.")
                }
            }
        } else {
            var found = false
            printWriter.appendln("not object; $mapperAsDeclaredType")
            for ((i, tp) in defaultSerializerTypePrim) {
                printWriter.appendln("default serializers: $i for $tp")
                if (types.isAssignable(mapperAsDeclaredType, types.erasure(i))) {
                    printWriter.appendln("assignable!")
                    serializers.put(types.erasure(tp), AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                        m.printMessage(L.WARNING, "Mapper for $tp has many default mappers! It was ${it.className}, now replaced by ${mapper.qualifiedName}.")
                    }
                    found = true
                }
            }
            for ((i, tp) in defaultParserTypePrim) {
                printWriter.appendln("default parsers: $i for $tp")
                if (types.isAssignable(mapperAsDeclaredType, types.erasure(i))) {
                    printWriter.appendln("assignable!")
                    parsers.put(types.erasure(tp), AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                        m.printMessage(L.WARNING, "Mapper for $tp has many default mappers! It was ${it.className}, now replaced by ${mapper.qualifiedName}.")
                    }
                    found = true
                }
            }
            if (!found) {
                printWriter.appendln("array?")
                // generic arrays (primitive arrays should implement MappingAdapter<x[]>
                if (types.isAssignable(mapperAsDeclaredType, defaultArrayParserType))
                    parsers[objectArrayType] =
                            AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()])
                if (types.isAssignable(mapperAsDeclaredType, defaultArraySerializerType))
                    serializers[objectArrayType] =
                            AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()])
            }
            if (!found) {
                printWriter.appendln("generics?")
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
                    printWriter.appendln("processing $method")
                    if (method.parameters.isEmpty()) continue
                    val parameters = method.parameters
                    if (method.typeParameters.isEmpty()) continue
                    val returnType = method.returnType
                    val typeParameters = method.typeParameters
                    val firstArgTp = parameters[0].asType()
                    printWriter.appendln("params: $parameters, retTp: $returnType, tpArgs: $typeParameters, firstArgTp: $firstArgTp")
                    if (method.simpleName.toString() == "toObject"
                            && parameters.size > 1
                            && types.isAssignable(generator.inputTypeName, firstArgTp)
                            && returnType is DeclaredType
                            && returnType.typeArguments.count { it is TypeVariable } == parameters.size - 1
                            && parameters.size - 1 == typeParameters.size
                            && run {
                                var correct = true
                                val retTpTA = returnType.typeArguments
                                printWriter.appendln("toObject? retTp type args: $retTpTA")
                                for (i in typeParameters.indices) {
                                    val retTpArg = retTpTA[i]
                                    val typeParameter = typeParameters[i].asType()
                                    val parserTp = parameters[i + 1].asType()
                                    printWriter.appendln("$i: retTp tp arg: $retTpArg, tp arg: $typeParameter, parserTp: $parserTp")
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
                        printWriter.appendln("got 'toObject'!")
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
                                printWriter.appendln("write?")
                                for (i in typeParameters.indices) {
                                    val typeParameter = typeParameters[i].asType()
                                    val serializerTp = parameters[i + 2].asType()
                                    printWriter.appendln("$i: tp arg: $typeParameter, serializerTp: $serializerTp")
                                    if (!(serializerTp is DeclaredType
                                                    && types.isSameType(types.erasure(serializerTp), types.erasure(defaultSerializerType))
                                                    && serializerTp.typeArguments.size == 2
                                                    && serializerTp.typeArguments[0] == typeParameter
                                                    && serializerTp.typeArguments[1] == outputTp)) {
                                        correct = false
                                        break
                                    }
                                }
                                correct
                            }) {
                        // actually, correct "write" signature
                        printWriter.appendln("got 'write'!")
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
                        printWriter.appendln("got 'apply'!")
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
                printWriter.appendln("following methods found: \n\twrite: $write\n\ttoObj: $toObject\n\tapplyPar: $applyPar\n\tapplySer: $applySer")
                for (app in applyPar) {
                    val erasure = types.erasure((app.returnType as DeclaredType).typeArguments[0])
                    for (toObj in toObject) {
                        if (types.isSameType(types.erasure(toObj.returnType), erasure)) {
                            parsers.put(erasure, AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                                m.printMessage(L.WARNING, "Mapper for $erasure has many default mappers! It was $it, now replaced by ${mapper.qualifiedName}.")
                            }
                            found = true
                        }
                    }
                }
                for (app in applySer) {
                    val erasure = types.erasure((app.returnType as DeclaredType).typeArguments[0])
                    for (w in write) {
                        if (types.isSameType(types.erasure(w.parameters[0].asType() as DeclaredType), erasure)) {
                            serializers.put(erasure, AdapterInfo(mapper.qualifiedName, instances[mapper.qualifiedName.toString()]))?.let {
                                m.printMessage(L.WARNING, "Mapper for $erasure has many default mappers! It was $it, now replaced by ${mapper.qualifiedName}.")
                            }
                            found = true
                        }
                    }
                }

            }
            printWriter.appendln("found? $found")
            printWriter.flush()
            if (!found) m.printMessage(L.ERROR, "${mapper.qualifiedName} annotated as DefaultMapper, but is not an MappingAdapter")
        }
        printWriter.appendln("end of processing $mapper (annotated by $annotationName)");
        printWriter.flush()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline infix fun Boolean.then(x: Boolean): Boolean = !this || (this && x)
}