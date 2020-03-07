/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Generators.
 *
 * Mapper Generators is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Generators is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Mapper Generators.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.gen;

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.DefaultParser;
import com.gitlab.faerytea.mapper.annotations.DefaultSerializer;
import com.gitlab.faerytea.mapper.converters.ConvertWrapper;
import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import static com.gitlab.faerytea.mapper.gen.SpecifiedMapper.AdapterType.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SimpleJsonGenerator extends SimpleGenerator {
    private final Set<AdapterInfo> instanceless = new HashSet<>();
    private final Map<AdapterInfo, String> adapterNames = new HashMap<>();
    private final Map<AdapterInfo, String> localAdapterNames = new HashMap<>();
    private final Set<FieldSpec> genericAdapterFields = new LinkedHashSet<>();
    private final Set<ClassName> circular = new HashSet<>();
    private final ClassName parserClass = ClassName.get(Parser.class);
    private final ClassName serializerClass = ClassName.get(Serializer.class);
    private final ClassName mapperClass = ClassName.get(MappingAdapter.class);
    private final TypeName inputClassName;
    private final TypeName outputClassName;
    private final TypeMirror stringType;
    private GeneratedResultInfo currentGenerated = null;

    protected SimpleJsonGenerator(@NotNull ProcessingEnvironment env,
                                  @NotNull CharSequence inputTypeName,
                                  @NotNull CharSequence outputTypeName) throws GeneratingException {
        super(env, inputTypeName, outputTypeName);
        inputClassName = TypeName.get(getInputTypeName());
        outputClassName = TypeName.get(getOutputTypeName());
        stringType = elemUtils.getTypeElement("java.lang.String").asType();
    }

    @NotNull
    @Override
    public GeneratedResultInfo generateFor(@NotNull TypeElement targetType,
                                           @NotNull Map<@NotNull String, @NotNull FieldData> fields,
                                           @NotNull AdapterInfo onUnknown,
                                           @Nullable ValidatorInfo validator) throws GeneratingException, IOException {
        return generateFor(targetType, fields, null, onUnknown, validator, false);
    }

    @Override
    @NotNull
    public GeneratedResultInfo generateFor(@NotNull TypeElement targetType,
                                           @NotNull ConcreteTypeResolver resolver,
                                           boolean markAsDefault,
                                           @Nullable ValidatorInfo validator) throws GeneratingException, IOException {
        return generateFor(targetType, null, resolver, null, validator, markAsDefault);
    }

    @Override
    @NotNull
    public GeneratedResultInfo generateFor(@NotNull TypeElement targetType,
                                           @NotNull Map<@NotNull String, @NotNull String> constants,
                                           @NotNull AdapterInfo onUnknown,
                                           @NotNull AdapterInfo stringParser,
                                           @NotNull AdapterInfo stringSerializer) throws IOException {
        currentGenerated = nameFor(targetType);
        final InstanceData instance = currentGenerated.adapter.instance;
        assert instance != null; // we know it
        final String adapterClassName = currentGenerated.adapter.className;
        //region enum init
        final TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.bestGuess(adapterClassName))
                .addModifiers(Modifier.PUBLIC);
        builder.addType(TypeSpec.classBuilder("Holder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addField(FieldSpec.builder(
                        ClassName.bestGuess(adapterClassName),
                        instance.instanceJavaName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $L()", adapterClassName)
                        .build())
                .build());
        final TypeName target = TypeName.get(targetType.asType());
        builder.addSuperinterface(parameterized(target, MAPPER));

        adapterNames.clear();
        localAdapterNames.clear();
        genericAdapterFields.clear();
        final String stringParserName = acceptAdapter(stringParser, stringType);
        final String stringSerializerName = acceptAdapter(stringSerializer, stringType);
        final String onUnknownName = adapterName(onUnknown);
        if (onUnknown.instance == null) instanceless.add(onUnknown);
        adapterNames.put(onUnknown, onUnknownName);
        //endregion
        //region enum parser
        final CodeBlock.Builder toObject = CodeBlock.builder()
                .addStatement("final String got = $L.toObject(in)", stringParserName)
                .beginControlFlow("switch (got)");
        for (final Map.Entry<@NotNull String, @NotNull String> e : constants.entrySet()) {
            toObject.addStatement("case $S: return $T.$L", e.getKey(), target, e.getValue());
        }
        toObject.addStatement("default: $L.handle(got, in); break", onUnknownName)
                .endControlFlow() // switch
                .addStatement("return null");
        //endregion
        //region enum serializer
        final CodeBlock.Builder write = CodeBlock.builder()
                .beginControlFlow("switch (object)");
        for (final Map.Entry<@NotNull String, @NotNull String> e : constants.entrySet()) {
            write.addStatement("case $L: $L.write($S, destination); break", e.getValue(), stringSerializerName, e.getKey());
        }
        write.addStatement("default: $L.write(object.toString(), destination); break", stringSerializerName)
                .endControlFlow(); // switch
        //endregion
        builder
                .addMethod(MethodSpec.methodBuilder("toObject")
                        .addParameter(inputClassName, "in", Modifier.FINAL)
                        .returns(target)
                        .addAnnotation(Override.class)
                        .addException(IOException.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addCode(toObject.build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("write")
                        .addParameter(target, "object", Modifier.FINAL)
                        .addParameter(outputClassName, "destination", Modifier.FINAL)
                        .returns(TypeName.VOID)
                        .addAnnotation(Override.class)
                        .addException(IOException.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addCode(write.build())
                        .build());
        appendAdapterFields(builder);
        JavaFile.builder(packageOf(targetType), builder.build()).build().writeTo(filer);
        return new GeneratedResultInfo(currentGenerated.adapter, true, true);
    }

    @NotNull
    private GeneratedResultInfo generateFor(@NotNull TypeElement targetType,
                                            @Nullable Map<@NotNull String, @NotNull FieldData> fields,
                                            @Nullable ConcreteTypeResolver subtypes,
                                            @Nullable AdapterInfo onUnknown,
                                            @Nullable ValidatorInfo validator,
                                            boolean markAsDefault) throws GeneratingException, IOException {
        if (!((subtypes == null && fields != null && onUnknown != null)
                || (subtypes != null && fields == null && onUnknown == null)))
            throw new AssertionError();
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
                        instance.instanceJavaName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $L()", adapterClassName)
                        .build())
                .build());
        adapterNames.clear();
        localAdapterNames.clear();
        genericAdapterFields.clear();
        boolean par, ser;
        final boolean parameterized = !targetType.getTypeParameters().isEmpty();
        final List<TypeVariableName> typeVariables = targetType.getTypeParameters().stream().map(TypeVariableName::get).collect(Collectors.toList());
        final TypeName targetTypeName = parameterized
                ? ParameterizedTypeName.get(ClassName.get(targetType), typeVariables.toArray(new TypeName[0]))
                : ClassName.get(targetType);
        if (fields != null) { // generate mapper
            if (onUnknown.instance == null) {
                instanceless.add(onUnknown);
            }
            ser = true;
            par = targetType.getKind() == ElementKind.CLASS && !targetType.getModifiers().contains(Modifier.ABSTRACT);
            String validatorName = null;
            if (validator != null) {
                if (validator instanceof ValidatorInfo.ValidatorClass) {
                    ValidatorInfo.ValidatorClass cv = (ValidatorInfo.ValidatorClass) validator;
                    final AdapterInfo asAdapterInfo = cv.asAdapterInfo();
                    if (cv.instance == null) {
                        instanceless.add(asAdapterInfo);
                    }
                    validatorName = adapterName(asAdapterInfo);
                    adapterNames.put(asAdapterInfo, validatorName);
                }
            }
            for (FieldData v : fields.values()) {
                ser &= !v.getters.isEmpty();
                final boolean thisCanGenerateParser = v.setters.stream().anyMatch(s -> s.typeResolver == null || s.typeResolver.variant == SubtypeResolver.Variant.WRAPPER_KEY);
                final boolean someoneCanGenerateParser = !v.setters.isEmpty();
                par &= thisCanGenerateParser;
                if (!thisCanGenerateParser && someoneCanGenerateParser) {
                    printSubtypesWarning(targetType, v);
                }
            }
            final String onUnknownName = adapterName(onUnknown);
            adapterNames.put(onUnknown, onUnknownName);
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
                } else if (!ser/* && par*/) {
                    builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), PARSER));
                } else /*if (ser && par)*/ {
                    builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), MAPPER));
                }
            }
            modify(builder, par, ser);
            if (par) {
                generateParser(targetType, fields, validator, adapterClassName, builder, validatorName, parameterized, typeVariables, targetTypeName, onUnknownName);
            }
            if (ser) {
                generateSerializer(targetType, fields, validator, adapterClassName, builder, validatorName, parameterized, typeVariables, targetTypeName);
            }
        } else { // resolver
            par = ser = true;
            for (final SpecifiedMapper mapper : subtypes.subtypes.values()) {
                par &= mapper.parser != null;
                ser &= mapper.serializer != null;
            }
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
                } else if (!ser/* && par*/) {
                    builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), PARSER));
                } else /*if (ser && par)*/ {
                    builder.addSuperinterface(parameterized(TypeName.get(targetType.asType()), MAPPER));
                }
            }
            modify(builder, par, ser);
            if (par) {
                final List<ParameterSpec> genericParams = typeVariables.stream().map(param -> ParameterSpec.builder(parameterized(param, PARSER), "var" + param.name, Modifier.FINAL).build()).collect(Collectors.toList());
                builder.addMethod(MethodSpec.methodBuilder("toObject")
                        .addTypeVariables(typeVariables)
                        .addParameter(inputClassName, "in", Modifier.FINAL)
                        .addParameters(genericParams)
                        .returns(targetTypeName)
                        .addException(IOException.class)
                        .addAnnotations(parameterized ? Collections.emptySet() : Collections.singleton(AnnotationSpec.builder(Override.class).build()))
                        .addModifiers(Modifier.PUBLIC)
                        .addCode(CodeBlock.builder()
                                .addStatement("final $T res", targetTypeName)
                                .add(writeResolverRead(subtypes, "res", subtypes.classGenerics))
                                .addStatement("return res")
                                .build())
                        .build());
                /* TODO: 26.02.20 validator */
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
                                                    .addParameter(inputClassName, "in", Modifier.FINAL)
                                                    .returns(targetTypeName)
                                                    .addAnnotation(Override.class)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addException(IOException.class)
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
                final List<ParameterSpec> genericParams = typeVariables.stream().map(param -> ParameterSpec.builder(parameterized(param, SERIALIZER), "var" + param.name, Modifier.FINAL).build()).collect(Collectors.toList());
                builder.addMethod(MethodSpec.methodBuilder("write")
                        .addTypeVariables(typeVariables)
                        .addParameter(targetTypeName, "object", Modifier.FINAL)
                        .addParameter(outputClassName, "destination", Modifier.FINAL)
                        .addParameters(genericParams)
                        .addAnnotations(parameterized ? Collections.emptySet() : Collections.singleton(AnnotationSpec.builder(Override.class).build()))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addException(IOException.class)
                        .addCode(writeResolverWrite(subtypes, "object", subtypes.classGenerics))
                        .build());
                /* TODO: 26.02.20 validator */
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
                                                    .addParameter(outputClassName, "destination", Modifier.FINAL)
                                                    .returns(TypeName.VOID)
                                                    .addAnnotation(Override.class)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addException(IOException.class)
                                                    .addCode(CodeBlock.builder()
                                                            .addStatement("$L.this.write(object, destination$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
                }
            }
            if (markAsDefault) {
                final Class<? extends Annotation> defaultMarker;
                if (!ser && !par) {
                    throw new GeneratingException("no serializer & no parser can be generated");
                } else if (ser && !par) {
                    defaultMarker = DefaultSerializer.class;
                } else if (!ser/* && par*/) {
                    defaultMarker = DefaultParser.class;
                } else /*if (ser && par)*/ {
                    defaultMarker = DefaultMapper.class;
                }
                builder.addAnnotation(defaultMarker);
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
                                            .addParameter(outputClassName, "destination", Modifier.FINAL)
                                            .returns(TypeName.VOID)
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(IOException.class)
                                            .addCode(CodeBlock.builder()
                                                    .addStatement("$L.this.write(object, destination$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                    .build())
                                            .build())
                                    .addMethod(MethodSpec.methodBuilder("toObject")
                                            .addParameter(inputClassName, "in", Modifier.FINAL)
                                            .returns(targetTypeName)
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(IOException.class)
                                            .addCode(CodeBlock.builder()
                                                    .addStatement("return $L.this.toObject(in$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build());

        }
        appendAdapterFields(builder);
        JavaFile.builder(packageOf(targetType), builder.build()).build().writeTo(filer);
        return new GeneratedResultInfo(currentGenerated.adapter, par, ser);
    }

    private void appendAdapterFields(@NotNull TypeSpec.Builder builder) {
        for (final Map.Entry<AdapterInfo, String> a : adapterNames.entrySet()) {
            if (a.getKey().className.equals(a.getValue())) continue;
            builder.addField(FieldSpec.builder(
                    ClassName.bestGuess(a.getKey().className),
                    a.getValue(),
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L", instanceOf(a.getKey()))
                    .build());
        }
        builder.addFields(genericAdapterFields);
    }

    private void printSubtypesWarning(@NotNull TypeElement targetType, @NotNull FieldData v) {
        class Msg {
            public final Element element;
            public final AnnotationMirror am;
            public final AnnotationValue av;

            Msg(Element element, AnnotationMirror am, AnnotationValue av) {
                this.element = element;
                this.am = am;
                this.av = av;
            }
        }
        messager.printMessage(Diagnostic.Kind.WARNING, "Parser can be generated in general, but cannot be generated by this generator; see following warnings");
        final List<? extends Element> allMembers = elemUtils.getAllMembers(targetType);
        for (final Setter setter: v.setters) {
            final String name;
            final ElementKind k;
            switch (setter.setterType) {
                case DIRECT:
                    k = ElementKind.FIELD;
                    name = setter.setterName;
                    break;
                case CONSTRUCTOR:
                    k = ElementKind.CONSTRUCTOR;
                    name = "<init>";
                    break;
                case BULK:
                case CLASSIC:
                    k = ElementKind.METHOD;
                    name = setter.setterName;
                    break;
                default:
                    throw new AssertionError("unknown enum " + setter.setterType);
            }
            allMembers.stream()
                    .filter(e -> e.getKind() == k
                            && e.getSimpleName().toString().equals(name))
                    .flatMap(e -> {
                        final SubtypeResolver a = e.getAnnotation(SubtypeResolver.class);
                        Stream<Msg> res = Stream.empty();
                        if (e instanceof ExecutableElement) {
                            final List<? extends VariableElement> parameters = ((ExecutableElement) e).getParameters();
                            res = parameters.stream()
                                    .flatMap(p -> {
                                        final List<? extends AnnotationMirror> mirrors = p.getAnnotationMirrors();
                                        for (final AnnotationMirror am : mirrors) {
                                            if (am.getAnnotationType().asElement().getSimpleName().toString().equals(SubtypeResolver.class.getSimpleName())) {
                                                final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = am.getElementValues();
                                                for (final AnnotationValue av: elementValues.values()) {
                                                    final Object val = av.getValue();
                                                    if (val instanceof SubtypeResolver.Variant) {
                                                        if (val != SubtypeResolver.Variant.WRAPPER_KEY) {
                                                            return Stream.of(new Msg(p, am, av));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        return Stream.empty();
                                    });
                        }
                        if (a != null) {
                            if (a.variant() != SubtypeResolver.Variant.WRAPPER_KEY) {
                                for (final AnnotationMirror am : e.getAnnotationMirrors()) {
                                    if (am.getAnnotationType().asElement().getSimpleName().toString().equals(SubtypeResolver.class.getSimpleName())) {
                                        for (final AnnotationValue av : am.getElementValues().values()) {
                                            final Object val = av.getValue();
                                            if (val instanceof SubtypeResolver) {
                                                res = Stream.concat(res, Stream.of(new Msg(e, am, av)));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return res;
                    })
                    .forEach(msg -> messager.printMessage(Diagnostic.Kind.WARNING,
                            "\tThis mapper generator does not support configuration which requires parsing context",
                            msg.element,
                            msg.am,
                            msg.av));

        }
    }

    private void generateParser(@NotNull TypeElement targetType,
                                @NotNull Map<@NotNull String, @NotNull FieldData> fields,
                                @Nullable ValidatorInfo validator,
                                String adapterClassName,
                                TypeSpec.Builder builder,
                                String validatorName,
                                boolean parameterized,
                                List<TypeVariableName> typeVariables,
                                TypeName targetTypeName,
                                String onUnknown) throws GeneratingException {
        final CodeBlock.Builder code = CodeBlock.builder();
        code.addStatement("final $1T res", targetType);
        code.add(initialAdvance());
        code.add("\n// start generated\n");
        //region parser init
        for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
            final String key = field.getKey();
            final FieldData data = field.getValue();
            final Setter setter = data.setters.get(0);
            code.addStatement("$T _$L = $L", data.fieldType, key, setter.defaultValue);
            final AdapterInfo adapter = setter.adapter;
            if (data.setters.get(0).typeResolver == null) acceptAdapter(adapter, data.fieldType);
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
            final ConverterData converter = setter.converter;
            final TypeName tmpTp = TypeName.get(converter == null ? data.fieldType : converter.from);
            code.beginControlFlow("case $S:", key)
                    .addStatement("final $T tmp", tmpTp);
            if (setter.typeResolver != null) {
                code.add(writeResolverRead(setter.typeResolver, "tmp", setter.genericArguments));
            } else {
                code.addStatement("tmp = $L.toObject(in$L)", get(setter.adapter), processGenerics(setter.genericArguments));
            }
            if (converter != null) {
                final String convName = adapterName(converter.converter);
                if (converter.converter.instance == null) instanceless.add(converter.converter);
                adapterNames.put(converter.converter, convName);
                code.addStatement("_$L = $L.$L(tmp)", key, convName, converter.decodeName());
            } else {
                code.addStatement("_$L = tmp", key);
            }
            code.addStatement("++cnt")
                    .addStatement("break")
                    .endControlFlow();
        }
        code.beginControlFlow("default:")
                .addStatement("$L.handle(name, in)", onUnknown)
                .endControlFlow()  // default
                .endControlFlow()  // switch
                .endControlFlow()  // else
                .endControlFlow(); // loop
        code.add("\n//endregion\n");
        //endregion
        //region assembling
        code.add("\n//region building\n");
        for (final Map.Entry<String, FieldData> entry : fields.entrySet()) {
            final FieldData data = entry.getValue();
            if (data.required) {
                final String def = data.setters.get(0).defaultValue;
                final String intermediate = data.fieldType.getKind().isPrimitive() || def.equals("null") ? "def == _$L" : "def.equals(_$L)";
                code.addStatement("if ($L) throw new IllegalStateException($S)", intermediate, entry.getKey(), "Field " + entry.getKey() + " is required!");
            }
        }
        final List<String> constParams = Generator.biggestConstructor(fields);
        for (final String param : constParams) {
            final FieldData data = fields.get(param);
            if (data.validator != null) {
                String instanceName = null;
                if (data.validator instanceof ValidatorInfo.ValidatorClass) {
                    final ValidatorInfo.ValidatorClass cv = (ValidatorInfo.ValidatorClass) data.validator;
                    final AdapterInfo e = cv.asAdapterInfo();
                    if (cv.instance == null) {
                        instanceless.add(e);
                    }
                    instanceName = adapterName(e);
                    adapterNames.put(e, instanceName);
                }
                code.add(data.validator.javaStatement(param, "constructor", targetTypeName.toString(), data.fieldType.toString(), '_' + param, instanceName))
                        .add("\n");
            }
        }
        code.addStatement("res = new $T($L)", targetType, joinParams(constParams));
        final Set<String> remaining = new HashSet<>(fields.keySet());
        remaining.removeAll(constParams);
        for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
            final String key = field.getKey();
            if (remaining.contains(key)) {
                final ValidatorInfo v = field.getValue().validator;
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
                    if (v != null) {
                        String instanceName = null;
                        if (v instanceof ValidatorInfo.ValidatorClass) {
                            final ValidatorInfo.ValidatorClass cv = (ValidatorInfo.ValidatorClass) v;
                            final AdapterInfo e = cv.asAdapterInfo();
                            if (cv.instance == null) {
                                instanceless.add(e);
                            }
                            instanceName = adapterName(e);
                            adapterNames.put(e, instanceName);
                        }
                        code.add(v.javaStatement(key, setter.setterName, targetTypeName.toString(), field.getValue().fieldType.toString(), '_' + key, instanceName))
                                .add("\n");
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
        if (validator != null) {
            code.add("\n");
            code.add(validator.javaStatement("<top level>", targetTypeName.toString(), targetTypeName.toString(), targetType.toString(), "res", validatorName));
            code.add("\n");
        }
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
                .addParameter(inputClassName, "in", Modifier.FINAL)
                .addParameters(genericParams)
                .returns(targetTypeName)
                .addException(IOException.class)
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
                                            .addParameter(inputClassName, "in", Modifier.FINAL)
                                            .returns(targetTypeName)
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(IOException.class)
                                            .addCode(CodeBlock.builder()
                                                    .addStatement("return $L.this.toObject(in$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build());
        }
    }

    private void generateSerializer(@NotNull TypeElement targetType, @NotNull Map<@NotNull String, @NotNull FieldData> fields, @Nullable ValidatorInfo validator, String adapterClassName, TypeSpec.Builder builder, String validatorName, boolean parameterized, List<TypeVariableName> typeVariables, TypeName targetTypeName) throws GeneratingException {
        final CodeBlock.Builder code = CodeBlock.builder();
        if (validator != null) {
            code.add(validator.javaStatement("<top level>", targetTypeName.toString(), targetTypeName.toString(), targetType.toString(), "object", validatorName));
        }
        final CodeBlock writeDelimiter;
        if (writeDelimiter().isEmpty()) {
            writeDelimiter = CodeBlock.of("");
        } else {
            code.addStatement("boolean __delimiter = false");
            writeDelimiter = CodeBlock.builder()
                    .beginControlFlow("if (__delimiter)")
                    .addStatement(writeDelimiter())
                    .nextControlFlow("else")
                    .addStatement("__delimiter = true")
                    .endControlFlow()
                    .build();
        }
        code.add(startObject());
        for (final Map.Entry<String, FieldData> field : fields.entrySet()) {
            final String key = field.getKey();
            final FieldData data = field.getValue();
            code.add("\n//region " + key + "\n");
            final Getter getter = data.getters.get(0);
            final String javaGetter = getter.isMethod ? getter.getterName + "()" : getter.getterName;
            code.addStatement("final $T _$L = object.$L", data.fieldType, key, javaGetter);
            if (!data.required) {
                final String def = getter.defaultValue;
                final boolean primitive = data.fieldType.getKind().isPrimitive();
                final boolean isNull = def.equals("null");
                final String condition;
                if (primitive || isNull) {
                    if (primitive && isNull) {
                        switch (data.fieldType.getKind()) {
                            case BOOLEAN:
                                condition = "!_$L";
                                break;
                            case BYTE:
                            case SHORT:
                            case INT:
                            case LONG:
                                condition = "0 != _$L";
                                break;
                            case CHAR:
                                condition = "'\0' != _$L";
                                break;
                            case FLOAT:
                            case DOUBLE:
                                condition = "0.0 != _$L";
                                break;
                            default:
                                // impossible,
                                env.getMessager().printMessage(Diagnostic.Kind.ERROR, data.fieldType + " is primitive, but has kind " + data.fieldType.getKind());
                                condition = "true";
                        }
                    } else {
                        condition = def + " != _$L";
                    }
                } else {
                    condition = "!java.util.Objects.equals(_$L, " + def + ")";
                }
                code.beginControlFlow("if (" + condition + ")", key);
            }
            if (data.validator != null) {
                String instanceName = null;
                if (data.validator instanceof ValidatorInfo.ValidatorClass) {
                    final ValidatorInfo.ValidatorClass cv = (ValidatorInfo.ValidatorClass) data.validator;
                    final AdapterInfo e = cv.asAdapterInfo();
                    if (cv.instance == null) {
                        instanceless.add(e);
                    }
                    instanceName = adapterName(e);
                    adapterNames.put(e, instanceName);
                }
                code.add(data.validator.javaStatement(key, javaGetter, targetTypeName.toString(), field.getValue().fieldType.toString(), '_' + key, instanceName))
                        .add("\n");
            }
            code.add(writeDelimiter);
            code.add(writeProperty(key));
            final CodeBlock.Builder expr = CodeBlock.builder();
            if (getter.converter != null) {
                final String convertName = adapterName(getter.converter.converter);
                adapterNames.put(getter.converter.converter, convertName);
                if (getter.converter.converter.instance == null)
                    instanceless.add(getter.converter.converter);
                expr.add("$L.$L(_$L)", convertName, getter.converter.encodeName(), key);
            } else {
                expr.add("_$L", key);
            }
            if (getter.typeResolver == null) {
                final String adaName = acceptAdapter(getter.adapter, data.fieldType);
                code.addStatement("$L.write($L, destination$L)", adaName, expr.build(), processGenerics(getter.genericArguments));
            } else {
                if (getter.converter != null) {
                    code.beginControlFlow("") // visibility scope
                            .addStatement("final $T tmp = $L", getter.converter.from, expr.build())
                            .add(writeResolverWrite(getter.typeResolver, "tmp", getter.genericArguments))
                            .endControlFlow();
                } else {
                    code.add(writeResolverWrite(getter.typeResolver, expr.build().toString(), getter.genericArguments));
                }
            }
            if (!data.required) {
                code.endControlFlow();
            }
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
                .addParameter(outputClassName, "destination", Modifier.FINAL)
                .addParameters(genericParams)
                .addAnnotations(parameterized ? Collections.emptySet() : Collections.singleton(AnnotationSpec.builder(Override.class).build()))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addException(IOException.class)
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
                                            .addParameter(outputClassName, "destination", Modifier.FINAL)
                                            .returns(TypeName.VOID)
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(IOException.class)
                                            .addCode(CodeBlock.builder()
                                                    .addStatement("$L.this.write(object, destination$L)", adapterClassName, genericParams.stream().map(param -> ", " + param.name).collect(Collectors.joining()))
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build());
        }
    }

    @NotNull
    private CodeBlock processGenerics(List<@NotNull GenericTypeInfo> genericArguments) throws GeneratingException {
        final CodeBlock.Builder generics = CodeBlock.builder();
        for (final GenericTypeInfo gti : genericArguments) {
            try {
                generics.add(", " + buildApplication(gti, false));
            } catch (WrappedException e) {
                e.unwrap();
            }
        }
        return generics.build();
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
    private ParameterizedTypeName parameterized(@NotNull TypeName target, @NotNull SpecifiedMapper.AdapterType type) {
        switch (type) {
            case PARSER:
                return ParameterizedTypeName.get(parserClass, target, inputClassName);
            case SERIALIZER:
                return ParameterizedTypeName.get(serializerClass, target, outputClassName);
            case MAPPER:
                return ParameterizedTypeName.get(mapperClass, target, inputClassName, outputClassName);
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
        return adapter.className.replace('.', '_').toUpperCase()
                + (adapter.instance != null && adapter.instance.namedInstanceName != null ? "__" + adapter.instance.namedInstanceName : "");
    }

    @NotNull
    private String joinParams(@NotNull Collection<@NotNull String> constParams) {
        return constParams.stream().map(n -> "_" + n).collect(Collectors.joining(", "));
    }

    private CodeBlock writeResolverRead(@NotNull ConcreteTypeResolver ctr,
                                        @NotNull String resultName,
                                        @NotNull List<GenericTypeInfo> generics) throws GeneratingException {
        if (ctr.variant != SubtypeResolver.Variant.WRAPPER_KEY)
            throw new GeneratingException("cannot generate resolver for variant " + ctr.variant);
        final CodeBlock.Builder builder = CodeBlock.builder()
                .add(initialAdvance())
                .addStatement("final String name;")
                .add(nextName())
                .beginControlFlow("if (name != null)")
                .beginControlFlow("switch (name)");
        final CodeBlock genericsAddition = processGenerics(generics);
        for (final Map.Entry<String, SpecifiedMapper> e : ctr.subtypes.entrySet()) {
            final SpecifiedMapper mapper = e.getValue();
            final AdapterInfo parser = mapper.parser;
            if (parser == null) throw new GeneratingException("got null parser for " + ctr);
            acceptAdapter(parser, mapper.type);
            builder.addStatement("case $S: $L = $L.toObject(in$L); break", e.getKey(), resultName, get(parser), genericsAddition);
        }
        if (ctr.defaultSubtype.parser != null) {
            acceptAdapter(ctr.defaultSubtype.parser, ctr.defaultSubtype.type);
            builder.addStatement("default: $L = ($T) $L.toObject(in); break", resultName, ctr.defaultSubtype.type, get(ctr.defaultSubtype.parser));
        }
        builder.endControlFlow() // switch
                .nextControlFlow("else") // if
                .addStatement("$L = null", resultName)
                .endControlFlow() // else
                .add(finalMove());
        return builder.build();
    }

    private CodeBlock writeResolverWrite(@NotNull ConcreteTypeResolver ctr,
                                         @NotNull String objectName,
                                         @NotNull List<GenericTypeInfo> generics) throws GeneratingException {
        final CodeBlock.Builder builder = CodeBlock.builder();
        final CodeBlock genericsAddition = processGenerics(generics);
        switch (ctr.variant) {
            case WRAPPER_KEY:
                builder.add(startObject())
                        .beginControlFlow("switch ($L.getClass().getCanonicalName())", objectName);
                for (Map.Entry<String, SpecifiedMapper> e : ctr.subtypes.entrySet()) {
                    final SpecifiedMapper mapper = e.getValue();
                    final AdapterInfo serializer = mapper.serializer;
                    if (serializer == null) throw new GeneratingException("got null serializer for " + ctr);
                    acceptAdapter(serializer, mapper.type);
                    builder.beginControlFlow("case $S:", mapper.type.toString())
                            .addStatement(writeProperty(e.getKey()))
                            .addStatement("$L.write(($T) $L, destination$L)", get(serializer), mapper.type, objectName, genericsAddition)
                            .addStatement("break")
                            .endControlFlow();
                }
                if (ctr.defaultSubtype.serializer != null) {
                    final SpecifiedMapper defaultSubtype = ctr.defaultSubtype;
                    acceptAdapter(defaultSubtype.serializer, defaultSubtype.type);
                    builder.beginControlFlow("default:")
                            .addStatement(writeProperty(defaultSubtype.type.toString()))
                            .addStatement("$L.write(($T) $L, destination$L)", get(defaultSubtype.serializer), defaultSubtype.type, objectName, genericsAddition)
                            .addStatement("break")
                            .endControlFlow();
                }
                builder.endControlFlow()
                        .add(endObject());
                break;
            case WRAPPER_PROPERTY:
                break;
            default:
                throw new GeneratingException(ctr.variant + " is not supported");
        }
        return builder.build();
    }

    @NotNull
    @Override
    public GeneratedResultInfo nameFor(@NotNull TypeElement targetType) {
        final String className = ClassName.get(targetType).reflectionName() + "Adapter";
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

    private ParameterizedTypeName interfaceType(@NotNull TypeName tp, @Nullable AdapterInfo parser, @Nullable AdapterInfo serializer) {
        if (parser != null) {
            if (serializer != null) {
                return ParameterizedTypeName.get(mapperClass, tp, inputClassName, outputClassName);
            } else {
                return ParameterizedTypeName.get(parserClass, tp, inputClassName);
            }
        } else {
            if (serializer != null) {
                return ParameterizedTypeName.get(serializerClass, tp, outputClassName);
            } else {
                throw new IllegalArgumentException("("+tp+",null,null)");
            }
        }
    }

    @NotNull
    private String buildApplication(@NotNull GenericTypeInfo gti, boolean isParser) throws WrappedException {
        final SpecifiedMapper.AdapterType collapsed = collapse(gti);
        String name, opposite = null;
        if (gti.typeResolver != null) {
            CodeBlock writeResolverRead = null;
            try {
                writeResolverRead = writeResolverRead(gti.typeResolver, "res", gti.nestedGeneric);
            } catch (GeneratingException e) {
                if (isParser || gti.adapterType != SERIALIZER) throw new WrappedException(e);
            }
            CodeBlock writeResolverWrite = null;
            try {
                writeResolverWrite = writeResolverWrite(gti.typeResolver, "object", gti.nestedGeneric);
            } catch (GeneratingException e) {
                if (!isParser || gti.adapterType != PARSER) throw new WrappedException(e);
            }
            final List<MethodSpec> methods = new ArrayList<>(2);
            final TypeName objectTp = TypeName.get(gti.mapper.type);
            if (gti.adapterType != SERIALIZER) {
                methods.add(MethodSpec.methodBuilder("toObject")
                        .addException(IOException.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(inputClassName, "in", Modifier.FINAL)
                        .returns(objectTp)
                        .addCode(CodeBlock.builder()
                                .addStatement("final res")
                                .add(writeResolverRead)
                                .addStatement("return res")
                                .build())
                        .build());
            }
            if (gti.adapterType != PARSER) {
                methods.add(MethodSpec.methodBuilder("write")
                        .addException(IOException.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(objectTp, "object", Modifier.FINAL)
                        .addParameter(outputClassName, "to", Modifier.FINAL)
                        .returns(TypeName.VOID)
                        .addCode(writeResolverWrite.toBuilder().addStatement("return res").build())
                        .build());
            }
            final ParameterizedTypeName interfaceType = interfaceType(objectTp, gti.mapper.parser, gti.mapper.serializer);
            final TypeSpec anon = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(interfaceType)
                    .addMethods(methods)
                    .build();
            final FieldSpec fieldSpec = FieldSpec.builder(interfaceType, "resolverFor_" + gti.mapper.type, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L;", anon)
                    .build();
            genericAdapterFields.add(fieldSpec);
            name = fieldSpec.name;
            if (gti.adapterType == MAPPER) opposite = name;
        } else {
            if (gti.nestedGeneric.isEmpty()) {
                final String finalNoArgParser = gti.mapper.parser == null ? null : acceptAdapter(gti.mapper.parser, gti.mapper.type);
                final String finalNoArgSerializer = gti.mapper.serializer == null ? null : acceptAdapter(gti.mapper.serializer, gti.mapper.type);
                name = isParser ? finalNoArgParser : finalNoArgSerializer;
                opposite = isParser ? finalNoArgSerializer : finalNoArgParser;
                if (name == null) throw new WrappedException(new GeneratingException("name is null"));
            } else {
                final List<String> fieldNames = gti.nestedGeneric.stream().map((GenericTypeInfo gti1) -> buildApplication(gti1, isParser)).collect(Collectors.toList());
                final AdapterInfo adapter = isParser ? gti.mapper.parser : gti.mapper.serializer;
                if (adapter == null) throw new WrappedException(new GeneratingException("isParser = " + isParser + ", " + gti.mapper));
                final String gName = adapter.className.replace('.', '_') + "__" + String.join("_", fieldNames) + "__";
                name = genericAdapterFields.stream().filter(spec -> spec.name.equals(gName)).map(s -> s.name).findFirst().orElseGet(() -> {
                    final FieldSpec fieldSpec = FieldSpec.builder(applicationFieldType(gti, collapsed), gName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L.apply(" + String.join(", ", fieldNames) + ")", instanceOf(adapter))
                            .build();
                    genericAdapterFields.add(fieldSpec);
                    return fieldSpec.name;
                });
                final AdapterInfo oppositeA = !isParser ? gti.mapper.parser : gti.mapper.serializer;
                if (adapter.equals(oppositeA)) {
                    opposite = name;
                } else if (oppositeA != null && collapsed == MAPPER) {
                    final List<String> oppositeFieldNames = gti.nestedGeneric.stream().map((GenericTypeInfo gti1) -> buildApplication(gti1, !isParser)).collect(Collectors.toList());
                    final String oppositeName = oppositeA.className.replace('.', '_') + "__" + String.join("_", oppositeFieldNames) + "__";
                    opposite = genericAdapterFields.stream().filter(spec -> spec.name.equals(oppositeName)).map(s -> s.name).findFirst().orElseGet(() -> {
                        final FieldSpec fieldSpec = FieldSpec.builder(applicationFieldType(gti, collapsed), oppositeName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("$L.apply(" + String.join(", ", oppositeFieldNames) + ")", instanceOf(oppositeA))
                                .build();
                        genericAdapterFields.add(fieldSpec);
                        return fieldSpec.name;
                    });
                }
            }
        }
        // check converter
        if (gti.converter != null) {
            final ParameterizedTypeName tp;
            final CodeBlock init;
            final String combinedName;
            final TypeName converterTarget = ClassName.get(gti.converter.to);
            final TypeName intermediateTp = ClassName.get(gti.converter.from);
            final String converter = acceptAdapter(gti.converter.converter, gti.converter.to);
            switch (collapsed) {
                case PARSER:
                    tp = ParameterizedTypeName.get(ClassName.get(ConvertWrapper.AsParser.class), converterTarget, intermediateTp, inputClassName);
                    init = CodeBlock.of("new $T($L, $L);", tp, name, converter);
                    combinedName = name;
                    break;
                case SERIALIZER:
                    tp = ParameterizedTypeName.get(ClassName.get(ConvertWrapper.AsSerializer.class), converterTarget, intermediateTp, outputClassName);
                    init = CodeBlock.of("new $T($L, $L);", tp, name, converter);
                    combinedName = name;
                    break;
                case MAPPER:
                    tp = ParameterizedTypeName.get(ClassName.get(ConvertWrapper.AsMapper.class), converterTarget, intermediateTp, inputClassName, outputClassName);
                    init = CodeBlock.of("new $T($L, $L, $L);", tp, name, opposite, converter);
                    if (name.equals(opposite)) {
                        combinedName = name;
                    } else {
                        if (isParser) {
                            combinedName = name + "_$_" + opposite;
                        } else {
                            combinedName = opposite + "_$_" + name;
                        }
                    }
                    break;
                default:
                    throw new AssertionError(gti.adapterType.toString());
            }
            final FieldSpec fieldSpec = FieldSpec.builder(tp, converter.replace('.', '_') + "__" + combinedName + "__", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(init)
                    .build();
            name = fieldSpec.name;
            genericAdapterFields.add(fieldSpec);
        }
        return name;
    }

    @NotNull
    private TypeName applicationFieldType(@NotNull GenericTypeInfo gti, @NotNull SpecifiedMapper.AdapterType adapterType) throws WrappedException {
        return parameterized(ClassName.get(gti.mapper.type), adapterType);
    }

    @NotNull
    private SpecifiedMapper.AdapterType collapse(@NotNull GenericTypeInfo gti) throws WrappedException {
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
        if (canParse && canSerialize) return SpecifiedMapper.AdapterType.MAPPER;
        if (canParse) return PARSER;
        if (canSerialize) return SpecifiedMapper.AdapterType.SERIALIZER;
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

    private static class MapperForGenerics {
        final String parserName;
        final String serializerName;

        private MapperForGenerics(String parserName, String serializerName) {
            this.parserName = parserName;
            this.serializerName = serializerName;
        }

        private MapperForGenerics(@NotNull String mapperName) {
            parserName = serializerName = mapperName;
        }
    }
}
