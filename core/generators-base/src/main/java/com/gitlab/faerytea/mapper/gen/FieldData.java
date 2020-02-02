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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.lang.model.type.TypeMirror;

/**
 * Info about field.
 */
public final class FieldData {
    /**
     * Name of field in serialized form.
     */
    @NotNull
    public final String fieldName;
    /**
     * Type of field.
     */
    @NotNull
    public final TypeMirror fieldType;
    @NotNull
    public final List<@NotNull Getter> getters;
    @NotNull
    public final List<@NotNull Setter> setters;
    public final boolean required;
    @Nullable
    public final ValidatorInfo validator;

    public FieldData(
            @NotNull String fieldName,
            @NotNull TypeMirror fieldType,
            @NotNull List<@NotNull Getter> getters,
            @NotNull List<@NotNull Setter> setters,
            boolean required,
            @Nullable ValidatorInfo validator) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.getters = getters;
        this.setters = setters;
        this.required = required;
        this.validator = validator;
    }

    @Override
    public String toString() {
        return "FieldData{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType=" + fieldType +
                ", getters=" + getters +
                ", setters=" + setters +
                ", required=" + required +
                ", validator=" + validator +
                '}';
    }
}
