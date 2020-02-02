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

import java.util.Collections;
import java.util.List;

import javax.lang.model.type.TypeMirror;

/**
 * Description how to set value
 */
public final class Setter {
    /**
     * Serialized names; list will contain at least one element, and exactly one if
     * {@link #setterType} is {@link Type#DIRECT} or {@link Type#CLASSIC}.
     * If there is two or more names then these order is same as in setter method.
     */
    @NotNull
    public final List<@NotNull String> propertyNames;
    /**
     * Setter name in java, could be name of field, method name
     */
    @NotNull
    public final String setterName;
    @NotNull
    public final Type setterType;
    @NotNull
    public final AdapterInfo adapter;
    @NotNull
    public final String defaultValue;
    @NotNull
    public final List<@NotNull GenericTypeInfo> genericArguments;
    @Nullable
    public final ConverterData converter;

    public Setter(@NotNull List<@NotNull String> propertyNames,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments,
                  @Nullable ConverterData converter) {
        this.propertyNames = propertyNames;
        this.setterName = setterName;
        this.setterType = setterType;
        this.adapter = adapter;
        this.defaultValue = defaultValue;
        this.genericArguments = genericArguments;
        this.converter = converter;
    }

    public Setter(@NotNull List<@NotNull String> propertyNames,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments) {
        this(propertyNames, setterName, setterType, adapter, defaultValue, genericArguments, null);
    }

    public Setter(@NotNull List<@NotNull String> propertyNames,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @Nullable ConverterData converter) {
        this(propertyNames, setterName, setterType, adapter, defaultValue, Collections.emptyList(), converter);
    }

    public Setter(@NotNull List<@NotNull String> propertyNames,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue) {
        this(propertyNames, setterName, setterType, adapter, defaultValue, Collections.emptyList(), null);
    }

    public Setter(@NotNull String fieldName,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments,
                  @Nullable ConverterData converter) {
        this(Collections.singletonList(fieldName), setterName, setterType, adapter, defaultValue, genericArguments, converter);
    }

    public Setter(@NotNull String fieldName,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments) {
        this(fieldName, setterName, setterType, adapter, defaultValue, genericArguments, null);
    }

    public Setter(@NotNull String fieldName,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @Nullable ConverterData converter) {
        this(Collections.singletonList(fieldName), setterName, setterType, adapter, defaultValue, converter);
    }

    public Setter(@NotNull String fieldName,
                  @NotNull String setterName,
                  @NotNull Type setterType,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue) {
        this(fieldName, setterName, setterType, adapter, defaultValue, Collections.emptyList(), null);
    }

    @NotNull
    @Override
    public String toString() {
        return "Setter{" +
                "propertyNames=" + propertyNames +
                ", setterName='" + setterName + '\'' +
                ", setterType=" + setterType +
                ", adapterName=" + adapter +
                ", defaultValue='" + defaultValue + '\'' +
                ", genericArguments=" + genericArguments +
                ", converter=" + converter +
                '}';
    }

    public enum Type {
        /**
         * Direct access to java class' field, that field is not final
         */
        DIRECT,
        /**
         * Simple setter, e.g. {@code setField(Tp value)}
         */
        CLASSIC,
        /**
         * Setter which consumes two or more serialized fields
         */
        BULK,
        /**
         * Constructor parameter
         */
        CONSTRUCTOR
    }
}
