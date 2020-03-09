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
package com.gitlab.faerytea.mapper.jackson;

import com.gitlab.faerytea.mapper.gen.AdapterInfo;
import com.gitlab.faerytea.mapper.gen.GeneratingException;
import com.gitlab.faerytea.mapper.gen.InstanceData;
import com.gitlab.faerytea.mapper.gen.SimpleJsonGenerator;
import com.gitlab.faerytea.mapper.gen.TypeInfo;
import com.squareup.javapoet.CodeBlock;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class JacksonProcessor extends SimpleJsonGenerator {
    private static final CodeBlock INITIAL_ADVANCE = CodeBlock.builder()
            .beginControlFlow("if (in.currentToken() == com.fasterxml.jackson.core.JsonToken.VALUE_NULL)")
            .addStatement("in.nextToken()")
            .addStatement("return null")
            .endControlFlow()
            .addStatement("in.nextToken()")
            .build();
    private static final CodeBlock NEXT_NAME = CodeBlock.builder()
            .beginControlFlow("if (in.currentToken() != com.fasterxml.jackson.core.JsonToken.FIELD_NAME)")
            .addStatement("name = null")
            .nextControlFlow("else")
            .addStatement("name = in.currentName()")
            .addStatement("in.nextToken()")
            .endControlFlow()
            .build();
    private static final CodeBlock FINAL_MOVE = CodeBlock.builder()
            .addStatement("if (in.currentToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) in.nextToken()")
            .addStatement("in.nextToken()")
            .build();
    private static final CodeBlock START_OBJECT = CodeBlock.builder()
            .beginControlFlow("if (object == null)")
            .addStatement("destination.writeNull()")
            .addStatement("return")
            .endControlFlow()
            .addStatement("destination.writeStartObject()")
            .build();
    private static final CodeBlock END_OBJECT = CodeBlock.builder()
            .addStatement("destination.writeEndObject()")
            .build();
    private static final CodeBlock EMPTY = CodeBlock.of("");
    private final Map<TypeInfo, AdapterInfo> defaultMappers = new HashMap<>();

    public JacksonProcessor(@NotNull ProcessingEnvironment env) throws GeneratingException {
        super(env, "com.fasterxml.jackson.core.JsonParser", "com.fasterxml.jackson.core.JsonGenerator");
        putTp("com.gitlab.faerytea.mapper.jackson.primitives.StringMapper", typeFrom(String.class));
        putPrim(TypeKind.BOOLEAN);
        putPrim(TypeKind.INT);
        putPrim(TypeKind.LONG);
        putPrim(TypeKind.DOUBLE);
        putClass(ArrayList.class);
        putClass(List.class);
        putClass(Set.class);
        putClass(Map.class);
    }

    @Override
    protected @NotNull CodeBlock initialAdvance() {
        return INITIAL_ADVANCE;
    }

    @Override
    protected @NotNull CodeBlock nextName() {
        return NEXT_NAME;
    }

    @Override
    protected @NotNull CodeBlock finalMove() {
        return FINAL_MOVE;
    }

    @Override
    protected @NotNull CodeBlock startObject() {
        return START_OBJECT;
    }

    @Override
    protected @NotNull CodeBlock endObject() {
        return END_OBJECT;
    }

    @Override
    protected @NotNull CodeBlock writeProperty(String name) {
        return CodeBlock.of("destination.writeFieldName($S);\n", name);
    }

    @Override
    protected CodeBlock writeDelimiter() {
        return EMPTY;
    }

    @Override
    public @NotNull Map<TypeInfo, AdapterInfo> getDefaultParsers() {
        return defaultMappers;
    }

    @Override
    public @NotNull Map<TypeInfo, AdapterInfo> getDefaultSerializers() {
        return defaultMappers;
    }

    private void putPrim(TypeKind tp) {
        String name = tp.name();
        putTp("com.gitlab.faerytea.mapper.jackson.primitives." + (name.charAt(0) + name.substring(1).toLowerCase()) + "Mapper",
                typeUtils.getPrimitiveType(tp));
    }

    private void putClass(Class<?> cls) {
        String name = cls.getSimpleName();
        putTp("com.gitlab.faerytea.mapper.jackson.collections." + name + "Mapper", typeFrom(cls));
    }

    private void putTp(String className, TypeMirror type) {
        defaultMappers.put(
                TypeInfo.from(type),
                new AdapterInfo(className, new InstanceData(className, "INSTANCE", false)));
    }
}
