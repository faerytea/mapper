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

/**
 * Description how to get value
 */
public final class Getter {
    /**
     * Serialized name
     */
    @NotNull
    public final String propertyName;
    /**
     * Name in code
     */
    @NotNull
    public final String getterName;
    /**
     * {@code true} if it is method, {@code false} if it is field access
     */
    public final boolean isMethod;
    @NotNull
    public final AdapterInfo adapter;
    @NotNull
    public final String defaultValue;
    @NotNull
    public final List<@NotNull GenericTypeInfo> genericArguments;
    @Nullable
    public final ConverterData converter;
    @Nullable
    public final ConcreteTypeResolver typeResolver;

    public Getter(@NotNull String propertyName,
                  @NotNull String getterName,
                  boolean isMethod,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments,
                  @Nullable ConverterData converter,
                  @Nullable ConcreteTypeResolver typeResolver) {
        this.propertyName = propertyName;
        this.getterName = getterName;
        this.isMethod = isMethod;
        this.adapter = adapter;
        this.defaultValue = defaultValue;
        this.genericArguments = genericArguments;
        this.converter = converter;
        this.typeResolver = typeResolver;
    }

    @NotNull
    @Override
    public String toString() {
        return "Getter{" +
                "propertyName='" + propertyName + '\'' +
                ", getterName='" + getterName + '\'' +
                ", isMethod=" + isMethod +
                ", adapter=" + adapter +
                ", defaultValue='" + defaultValue + '\'' +
                ", genericArguments=" + genericArguments +
                ", converter=" + converter +
                '}';
    }
}
