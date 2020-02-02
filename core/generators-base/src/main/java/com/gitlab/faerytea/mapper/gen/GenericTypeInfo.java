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

import java.util.List;
import java.util.Objects;

import javax.lang.model.type.TypeMirror;

public class GenericTypeInfo {
    @NotNull
    public final TypeMirror type;
    @NotNull
    public final AdapterInfo adapter;
    @NotNull
    public final List<GenericTypeInfo> nestedGeneric;
    @NotNull
    public final AdapterType adapterType;

    public GenericTypeInfo(@NotNull TypeMirror type, @NotNull AdapterInfo adapter, @NotNull List<GenericTypeInfo> nestedGeneric, @NotNull AdapterType adapterType) {
        this.type = type;
        this.adapter = adapter;
        this.nestedGeneric = nestedGeneric;
        this.adapterType = adapterType;
    }

    @Override
    public String toString() {
        return "GenericTypeInfo{" +
                "type=" + type +
                ", adapter=" + adapter +
                ", nestedGeneric=" + nestedGeneric +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericTypeInfo that = (GenericTypeInfo) o;
        return type.equals(that.type) &&
                adapter.equals(that.adapter) &&
                nestedGeneric.equals(that.nestedGeneric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, adapter, nestedGeneric);
    }

    public enum AdapterType {
        PARSER, SERIALIZER, MAPPER
    }
}
