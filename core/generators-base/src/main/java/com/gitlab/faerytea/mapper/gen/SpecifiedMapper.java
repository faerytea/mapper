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

import java.util.Objects;

import javax.lang.model.type.TypeMirror;

/**
 * Resolved {@link com.gitlab.faerytea.mapper.annotations.SpecificMapper}.
 */
public class SpecifiedMapper {
    @NotNull
    public final TypeMirror type;
    @Nullable
    public final AdapterInfo parser;
    @Nullable
    public final AdapterInfo serializer;

    @NotNull
    public static AdapterType getAdapterType(@Nullable AdapterInfo parser, @Nullable AdapterInfo serializer) {
        if (parser != null) {
            if (serializer != null) return AdapterType.MAPPER;
            else return AdapterType.PARSER;
        } else {
            if (serializer != null) return AdapterType.SERIALIZER;
            else throw new IllegalArgumentException("both parser & serializer is nulls");
        }
    }

    public SpecifiedMapper(@NotNull TypeMirror type,
                           @Nullable AdapterInfo parser,
                           @Nullable AdapterInfo serializer) {
        this.type = type;
        this.parser = parser;
        this.serializer = serializer;
    }

    @NotNull
    public AdapterType adapterType() {
        return getAdapterType(parser, serializer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpecifiedMapper that = (SpecifiedMapper) o;
        return type.equals(that.type) &&
                Objects.equals(parser, that.parser) &&
                Objects.equals(serializer, that.serializer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, parser, serializer);
    }

    @Override
    public String toString() {
        return "SpecifiedMapper{" +
                "type=" + type +
                ", parser=" + parser +
                ", serializer=" + serializer +
                '}';
    }

    public enum AdapterType {
        PARSER, SERIALIZER, MAPPER
    }
}
