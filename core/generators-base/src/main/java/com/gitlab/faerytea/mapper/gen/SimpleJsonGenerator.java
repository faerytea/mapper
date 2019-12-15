package com.gitlab.faerytea.mapper.gen;

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import static com.gitlab.faerytea.mapper.gen.GenericTypeInfo.AdapterType.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SimpleJsonGenerator extends SimpleGenerator {
    private final Set<AdapterInfo> instanceless = new HashSet<>();
    private final Map<AdapterInfo, String> adapterNames = new HashMap<>();
    private final Map<AdapterInfo, String> localAdapterNames = new HashMap<>();
    private final List<FieldSpec> genericAdapterFields = new ArrayList<>();
    private final Set<ClassName> circular = new HashSet<>();
    private final ClassName parserClass = ClassName.get(Parser.class);
    private final ClassName serializerClass = ClassName.get(Serializer.class);
    private final ClassName mapperClass = ClassName.get(MappingAdapter.class);
    private GeneratedResultInfo currentGenerated = null;

    protected SimpleJsonGenerator(@NotNull ProcessingEnvironment env,
                                  @NotNull CharSequence inputTypeName,
                                  @NotNull CharSequence outputTypeName) throws GeneratingException {
        super(env, inputTypeName, outputTypeName);
    }

    @NotNull
    @Override
    public GeneratedResultInfo generateFor(@NotNull TypeElement targetType,
                                           @NotNull Map<@NotNull String, @NotNull FieldData> fields) throws GeneratingException, IOException {
        currentGenerated = nameFor(targetType);
        final InstanceData instance = currentGenerated.adapter.instance;
        assert instance != null; // we know it
        final String adapterClassName = currentGenerated.adapter.className;
        final TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.bestGuess(adapterClassName))
                .addModifiers(Modifier.PUBLIC);
        builder.addType(TypeSpec.classBuilder("Holder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addField(FieldSpec.builder(
                        ClassName.bestGuess(adapterClassName),
                        instance.instanceName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new " + adapterClassName + "()")
                        .build())
                .build());
        boolean ser = true, par = true;
        for (FieldData v : fields.values()) {
            ser &= !v.getters.isEmpty();
            par &= !v.setters.isEmpty();
        }
        adapterNames.clear();
        localAdapterNames.clear();
        genericAdapterFields.clear();
        final boolean parameterized = !targetType.getTypeParameters().isEmpty();
        final List<TypeVariableName> typeVariables = targetType.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        if (parameterized) {
            for (final TypeVariableName e : typeVariables) {
                final String parameterName = "var" + e.name;
                adapterNames.put(new AdapterInfo(parameterName), parameterName);
            }
        } else {
            if (!ser && !par) {
                throw new GeneratingException("no serializer & no parser can be generated");
            } else if (ser && !par) {
                builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), SERIALIZER));
            } else if (!ser && par) {
                builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), PARSER));
            } else if (ser && par) {
                builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), MAPPER));
            }
        }
        modify(builder, par, ser);
        final TypeName targetTypeName = parameterized
                ? ParameterizedTypeName.get(ClassName.get(targetType), typeVariables.toArray(new TypeName[typeVariables.size()]))
                : ClassName.get(targetType);
        if (par) {
            final CodeBlock.Builder code = CodeBlock.builder();
            code.addStatement("final $1T res", targetType);
            code.add(initialAdvance());
            code.add("\n// start generated\n");
            //region parser init
            for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
                final String key = field.getKey();
                final FieldData data = field.getValue();
                final Setter setter = data.setters.get(0);
                code.addStatement("$T _$L = " + setter.defaultValue, data.fieldType, key);
                final AdapterInfo adapter = setter.adapter;
                acceptAdapter(adapter, data.fieldType);
            }
            //endregion
            //region parse loop
            code.add("\n//region parsing\n")
                    .addStatement("int cnt = 0")
                    .beginControlFlow("while (cnt < $L)", fields.size())
                    .addStatement("final String name")
                    .add(nextName())
                    .beginControlFlow("if (name == null)")
                    .addStatement("break")
                    .nextControlFlow("else")
                    .beginControlFlow("switch (name)");
            for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
                final String key = field.getKey();
                final FieldData data = field.getValue();
                final Setter setter = data.setters.get(0);
                code.beginControlFlow("case $S:", key);
                final CodeBlock.Builder generics = CodeBlock.builder();
                for (final GenericTypeInfo gti : setter.genericArguments) {
                    try {
                        generics.add(", " + buildApplication(gti));
                    } catch (WrappedException e) {
                        e.unwrap();
                    }
                }
                final ConverterData converter = setter.converter;
                if (converter != null) {
                    final String convName = adapterName(converter.converter);
                    if (converter.converter.instance == null) instanceless.add(converter.converter);
                    adapterNames.put(converter.converter, convName);
                    code.addStatement("_$L = $L.$L($L.toObject(in$L))", key, convName, converter.decodeName(), get(setter.adapter), generics.build());
                } else {
                    code.addStatement("_$L = $L.toObject(in$L)", key, get(setter.adapter), generics.build());
                }
                code.addStatement("++cnt")
                        .addStatement("break")
                        .endControlFlow();
            }
            code.beginControlFlow("default:")
                    .addStatement("java.lang.System.out.println($S + name)", "unknown property: ")
                    .endControlFlow()  // default
                    .endControlFlow()  // switch
                    .endControlFlow()  // else
                    .endControlFlow(); // loop
            code.add("\n//endregion\n");
            //endregion
            //region assembling
            code.add("\n//region building\n");
            final List<String> constParams = Generator.biggestConstructor(fields);
            code.addStatement("res = new $T($L)", targetType, joinParams(constParams));
            final Set<String> remaining = new HashSet<>(fields.keySet());
            remaining.removeAll(constParams);
            for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
                final String key = field.getKey();
                if (remaining.contains(key)) {
                    for (final Setter setter : field.getValue().setters) {
                        switch (setter.setterType) {
                            case DIRECT:
                                code.addStatement("res.$L = _$L", setter.setterName, key);
                                break;
                            case CLASSIC:
                                code.addStatement("res.$L(_$L)", setter.setterName, key);
                                break;
                            case BULK:
                                if (remaining.containsAll(setter.propertyNames)) {
                                    code.addStatement("res.$L($L)", setter.setterName, joinParams(setter.propertyNames));
                                    remaining.removeAll(setter.propertyNames);
                                } else {
                                    continue;
                                }
                                break;
                            case CONSTRUCTOR:
                                continue;
                        }
                        remaining.remove(key);
                        break;
                    }
                }
            }
            if (!remaining.isEmpty())
                throw new GeneratingException("Failed generation: cannot set " + remaining);
            code.add("\n//endregion\n");
            code.add(finalMove());
            code.addStatement("return res");
            //endregion
            final CodeBlock.Builder varInit = CodeBlock.builder();
            if (!localAdapterNames.isEmpty()) {
                for (final Map.Entry<AdapterInfo, String> a : localAdapterNames.entrySet()) {
                    if (a.getKey().className.equals(a.getValue()) || a.getValue().equals("this"))
                        continue;
                    varInit.addStatement("final $T $L = $L", ClassName.bestGuess(a.getKey().className), a.getValue(), instanceOf(a.getKey()));
                }
            }
            final List<ParameterSpec> genericParams = typeVariables.stream().map(param -> ParameterSpec.builder(parameterized(param, PARSER), "var" + param.name, Modifier.FINAL).build()).collect(Collectors.toList());
            builder.addMethod(MethodSpec.methodBuilder("toObject")
                    .addTypeVariables(typeVariables)
                    .addParameter(ClassName.get(getInputTypeName()), "in", Modifier.FINAL)
                    .addParameters(genericParams)
                    .returns(targetTypeName)
                    .addAnnotations(parameterized ? Collections.emptySet() : Collections.singleton(AnnotationSpec.builder(Override.class).build()))
                    .addModifiers(Modifier.PUBLIC)
                    .addCode(varInit.add(code.build()).build())
                    .build());

            if (parameterized) {
                builder.addMethod(MethodSpec.methodBuilder("apply")
                        .addModifiers(Modifier.PUBLIC)
                        .addTypeVariables(typeVariables)
                        .addParameters(genericParams)
                        .returns(parameterized(targetTypeName, PARSER))
                        .addCode(CodeBlock.builder()
                                .addStatement("return $L", TypeSpec.anonymousClassBuilder("")
                                        .addSuperinterface(parameterized(targetTypeName, PARSER))
                                        .addMethod(MethodSpec.methodBuilder("toObject")
                                                .addParameter(ClassName.get(getInputTypeName()), "in", Modifier.FINAL)
                                                .returns(targetTypeName)
                                                .addAnnotation(Override.class)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addCode(CodeBlock.builder()
                                                        .addStatement("return $L.this.toObject(in$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());
            }
        }
        if (ser) {
            final CodeBlock.Builder code = CodeBlock.builder();
            code.add(startObject());
            boolean writeDelimiter = false;
            for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
                final String key = field.getKey();
                final FieldData data = field.getValue();
                if (writeDelimiter) {
                    code.add(writeDelimiter());
                } else {
                    writeDelimiter = true;
                }
                code.add("\n//region " + key + "\n");
                code.add(writeProperty(key));
                final Getter getter = data.getters.get(0);
                final String adaName = acceptAdapter(getter.adapter, data.fieldType);
                final CodeBlock.Builder generics = CodeBlock.builder();
                for (final GenericTypeInfo gti : getter.genericArguments) {
                    try {
                        generics.add(", " + buildApplication(gti));
                    } catch (WrappedException e) {
                        e.unwrap();
                    }
                }
                final CodeBlock.Builder expr = CodeBlock.builder();
                final String javaGetter = getter.isMethod ? getter.getterName + "()" : getter.getterName;
                if (getter.converter != null) {
                    final String convertName = adapterName(getter.converter.converter);
                    adapterNames.put(getter.converter.converter, convertName);
                    if (getter.converter.converter.instance == null)
                        instanceless.add(getter.converter.converter);
                    expr.add("$L.$L(object.$L)", convertName, getter.converter.encodeName(), javaGetter);
                } else {
                    expr.add("object.$L", javaGetter);
                }
                code.addStatement("$L.write($L, destination$L)", adaName, expr.build(), generics.build());
                code.add("\n//endregion\n");
            }
            code.add(endObject());

            final CodeBlock.Builder varInit = CodeBlock.builder();
            if (!localAdapterNames.isEmpty()) {
                for (final Map.Entry<AdapterInfo, String> a : localAdapterNames.entrySet()) {
                    if (a.getKey().className.equals(a.getValue()) || a.getValue().equals("this"))
                        continue;
                    varInit.addStatement("final $T $L = $L", ClassName.bestGuess(a.getKey().className), a.getValue(), instanceOf(a.getKey()));
                }
            }
            final List<ParameterSpec> genericParams = typeVariables.stream().map(param -> ParameterSpec.builder(parameterized(param, SERIALIZER), "var" + param.name, Modifier.FINAL).build()).collect(Collectors.toList());
            builder.addMethod(MethodSpec.methodBuilder("write")
                    .addTypeVariables(typeVariables)
                    .addParameter(targetTypeName, "object", Modifier.FINAL)
                    .addParameter(ClassName.get(getOutputTypeName()), "destination", Modifier.FINAL)
                    .addParameters(genericParams)
                    .addAnnotations(parameterized ? Collections.emptySet() : Collections.singleton(AnnotationSpec.builder(Override.class).build()))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addCode(varInit.add(code.build()).build())
                    .build());

            if (parameterized) {
                builder.addMethod(MethodSpec.methodBuilder("apply")
                        .addModifiers(Modifier.PUBLIC)
                        .addTypeVariables(typeVariables)
                        .addParameters(genericParams)
                        .returns(parameterized(targetTypeName, SERIALIZER))
                        .addCode(CodeBlock.builder()
                                .addStatement("return $L", TypeSpec.anonymousClassBuilder("")
                                        .addSuperinterface(parameterized(targetTypeName, SERIALIZER))
                                        .addMethod(MethodSpec.methodBuilder("write")
                                                .addParameter(targetTypeName, "object", Modifier.FINAL)
                                                .addParameter(ClassName.get(getOutputTypeName()), "destination", Modifier.FINAL)
                                                .returns(TypeName.VOID)
                                                .addAnnotation(Override.class)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addCode(CodeBlock.builder()
                                                        .addStatement("$L.this.write(object, destination$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());
            }
        }
        if (parameterized && par && ser) {
            // apply for mapper
            final List<ParameterSpec> genericParams = typeVariables.stream().map(param -> ParameterSpec.builder(parameterized(param, MAPPER), "var" + param.name, Modifier.FINAL).build()).collect(Collectors.toList());
            builder.addMethod(MethodSpec.methodBuilder("apply")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariables(typeVariables)
                    .addParameters(genericParams)
                    .returns(parameterized(targetTypeName, MAPPER))
                    .addCode(CodeBlock.builder()
                            .addStatement("return $L", TypeSpec.anonymousClassBuilder("")
                                    .addSuperinterface(parameterized(targetTypeName, MAPPER))
                                    .addMethod(MethodSpec.methodBuilder("write")
                                            .addParameter(targetTypeName, "object", Modifier.FINAL)
                                            .addParameter(ClassName.get(getOutputTypeName()), "destination", Modifier.FINAL)
                                            .returns(TypeName.VOID)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addCode(CodeBlock.builder()
                                                    .addStatement("$L.this.write(object, destination$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                    .build())
                                            .build())
                                    .addMethod(MethodSpec.methodBuilder("toObject")
                                            .addParameter(ClassName.get(getInputTypeName()), "in", Modifier.FINAL)
                                            .returns(targetTypeName)
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addCode(CodeBlock.builder()
                                                    .addStatement("return $L.this.toObject(in$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build());

        }
        for (final Map.Entry<AdapterInfo, String> a : adapterNames.entrySet()) {
            if (a.getKey().className.equals(a.getValue())) continue;
            builder.addField(FieldSpec.builder(
                    ClassName.bestGuess(a.getKey().className),
                    a.getValue(),
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(instanceOf(a.getKey()))
                    .build());
        }
        builder.addFields(genericAdapterFields);
        JavaFile.builder(packageOf(targetType), builder.build()).build().writeTo(filer);
        return new GeneratedResultInfo(currentGenerated.adapter, par, ser);
    }

    @NotNull
    private String acceptAdapter(AdapterInfo adapter, TypeMirror tp) {
        if (adapterNames.containsKey(adapter) || localAdapterNames.containsKey(adapter)) {
            try {
                return get(adapter);
            } catch (GeneratingException e) {
                throw new AssertionError("unreachable");
            }
        }
        final String adapterName;
        if (adapter.equals(currentGenerated.adapter)) {
            adapterName = "this";
        } else if (!tp.getKind().isPrimitive() && circular.contains(ClassName.bestGuess(typeUtils.erasure(tp).toString()))) {
            adapterName = adapterName(adapter);
            localAdapterNames.put(adapter, adapterName);
        } else {
            if (adapter.instance == null) instanceless.add(adapter);
            adapterName = adapterName(adapter);
            adapterNames.put(adapter, adapterName);
        }
        return adapterName;
    }

    @NotNull
    private String get(AdapterInfo adapter) throws GeneratingException {
        final String res = adapterNames.get(adapter);
        if (res != null) return res;
        final String local = localAdapterNames.get(adapter);
        if (local != null) return local;
        if (adapter.equals(currentGenerated.adapter)) return "this";
        throw new GeneratingException("adapter " + adapter + " is not found");
    }

    @NotNull
    private ParameterizedTypeName parameterized(@NotNull TypeName target, @NotNull GenericTypeInfo.AdapterType type) {
        switch (type) {
            case PARSER:
                return ParameterizedTypeName.get(parserClass, target, TypeName.get(inputType));
            case SERIALIZER:
                return ParameterizedTypeName.get(serializerClass, target, TypeName.get(outputType));
            case MAPPER:
                return ParameterizedTypeName.get(mapperClass, target, TypeName.get(inputType), TypeName.get(outputType));
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    @Override
    public void notifyCircularDependency(@NotNull List<@NotNull TypeElement> cycle) {
        for (final TypeElement m : cycle) {
            circular.add(ClassName.get(m));
        }
    }

    @NotNull
    private String adapterName(@NotNull AdapterInfo adapter) {
        return adapter.className.replace('.', '_').toUpperCase();
    }

    @NotNull
    private String joinParams(@NotNull Collection<@NotNull String> constParams) {
        return constParams.stream().map(n -> "_" + n).collect(Collectors.joining(", "));
    }

    @NotNull
    @Override
    public GeneratedResultInfo nameFor(@NotNull TypeElement targetType) {
        final String className = packageOf(targetType) + '.' + targetType.getSimpleName() + "Adapter";
        return new GeneratedResultInfo(
                new AdapterInfo(
                        className,
                        new InstanceData(className + ".Holder", "INSTANCE", false)
                ),
                true,
                true
        );
    }

    @Override
    public void writeEpilogue() {
        for (final AdapterInfo adapter : instanceless) {
            final ClassName type = ClassName.bestGuess(adapter.className);
            try {
                JavaFile.builder(
                        type.packageName(),
                        TypeSpec.classBuilder(ClassName.bestGuess(adapter.className + "Holder"))
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .addField(FieldSpec.builder(type, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                        .initializer("new $T();", type)
                                        .build())
                                .addMethod(MethodSpec.constructorBuilder()
                                        .addModifiers(Modifier.PRIVATE)
                                        .addCode(CodeBlock.of("throw new AssertionError($S);", "cannot be constructed"))
                                        .build())
                                .build())
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
                messager.printMessage(Diagnostic.Kind.ERROR, "Cannot finish writing " + type + ": " + e.getMessage());
            }
        }
    }

    /**
     * Override this method if you need some additional manipulation with
     * generated class.
     *
     * @param adapterClass generated class
     * @param canParse     will it be capable of parsing
     * @param canSerialize will it be capable of serializing
     */
    protected void modify(@NotNull TypeSpec.Builder adapterClass, boolean canParse, boolean canSerialize) {
        // nothing to do
    }

    /**
     * Java code which put next name into final variable {@code name}
     * of type {@code String} and modifies parser's state to be
     * in start of next object.
     * If that name is not present expression must evaluate to
     * {@code null} and parser should point right after object.
     *
     * @return expression which returns next name
     */
    @NotNull
    protected abstract CodeBlock nextName();

    /**
     * Will be placed at start of parsing method
     *
     * @return prelude of parsing
     */
    @NotNull
    protected CodeBlock initialAdvance() {
        return CodeBlock.of("");
    }

    /**
     * Will be placed at end of parsing method.
     * After evaluating returned code parser must
     * point right after the end of object.
     *
     * @return epilogue of parsing
     */
    @NotNull
    protected CodeBlock finalMove() {
        return CodeBlock.of("");
    }

    /**
     * Writes start of {@code object} to {@code destination}.
     * Will be called first in {@link Serializer#write(Object, Object)}
     * method implementation.
     *
     * @return code for writing start of object.
     */
    @NotNull
    protected abstract CodeBlock startObject();

    /**
     * Writes end of {@code object} to {@code destination}.
     * Will be called last in {@link Serializer#write(Object, Object)}
     * method implementation.
     *
     * @return code for writing end of object.
     */
    @NotNull
    protected abstract CodeBlock endObject();

    /**
     * Writes property name to destination. See underlined parts
     * in following JSON example:
     * <pre>
     *     {
     *         <u>"order": </u>[{
     *             <u>"price": </u>17.0,
     *             <u>"name": </u>"potato"
     *         }],
     *         <u>"name": </u>"Bob",
     *         <u>"timestamp": </u>1555555555555
     *     }
     * </pre>
     *
     * @param name name of property
     * @return code for writing property names
     */
    @NotNull
    protected abstract CodeBlock writeProperty(final String name);

    /**
     * Writes JSON delimiter, comma (','), to destination.
     *
     * @return code block which writes comma
     */
    protected abstract CodeBlock writeDelimiter();

    private String instanceOf(@NotNull AdapterInfo adapter) {
        return adapter.instance == null
                ? adapter.className + "Holder.INSTANCE"
                : adapter.instance.javaAccessor();
    }

    private String buildApplication(@NotNull GenericTypeInfo gti) throws WrappedException {
        if (gti.nestedGeneric.isEmpty()) {
            return acceptAdapter(gti.adapter, gti.type);
        }
        final List<String> fieldNames = gti.nestedGeneric.stream().map(this::buildApplication).collect(Collectors.toList());
        final String name = gti.adapter.className.replace('.', '_') + "__" + String.join("_", fieldNames) + "__";
        return genericAdapterFields.stream().filter(spec -> spec.name.equals(name)).map(s -> s.name).findFirst().orElseGet(() -> {
            final FieldSpec fieldSpec = FieldSpec.builder(applicationFieldType(gti), name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L.apply(" + String.join(", ", fieldNames) + ")", instanceOf(gti.adapter))
                    .build();
            genericAdapterFields.add(fieldSpec);
            return fieldSpec.name;
        });
    }

    private TypeName applicationFieldType(@NotNull GenericTypeInfo gti) throws WrappedException {
        return parameterized(ClassName.get(gti.type), collapse(gti));
    }

    private GenericTypeInfo.AdapterType collapse(@NotNull GenericTypeInfo gti) throws WrappedException {
        boolean canParse = true, canSerialize = true;
        for (final GenericTypeInfo i : gti.nestedGeneric) {
            switch (collapse(i)) {
                case SERIALIZER:
                    canParse = false;
                    break;
                case PARSER:
                    canSerialize = false;
                    break;
            }
        }
        switch (gti.adapterType) {
            case SERIALIZER:
                canParse = false;
                break;
            case PARSER:
                canSerialize = false;
                break;
        }
        if (canParse && canSerialize) return GenericTypeInfo.AdapterType.MAPPER;
        if (canParse) return PARSER;
        if (canSerialize) return GenericTypeInfo.AdapterType.SERIALIZER;
        throw new WrappedException(new GeneratingException(gti + " is not collapsible to Parser / MappingAdapter / Serializer"));
    }

    protected static class WrappedException extends RuntimeException {
        public WrappedException(GeneratingException cause) {
            super(cause);
        }

        @Override
        public synchronized GeneratingException getCause() {
            return (GeneratingException) super.getCause();
        }

        public void unwrap() throws GeneratingException {
            if (getCause() != null) throw getCause();
            else throw this;
        }
    }

//    private static final class CompositionBuilder {
//        private final TypeSpec.Builder
//    }
}
