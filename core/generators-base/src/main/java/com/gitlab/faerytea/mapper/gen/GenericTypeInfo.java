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
import java.util.Objects;

import javax.lang.model.type.TypeMirror;

public final class GenericTypeInfo {
    @NotNull
    public final SpecifiedMapper mapper;
    @NotNull
    public final List<GenericTypeInfo> nestedGeneric;
    @NotNull
    public final SpecifiedMapper.AdapterType adapterType;
    @Nullable
    public final ConverterData converter;
    @Nullable
    public final ConcreteTypeResolver typeResolver;

    public GenericTypeInfo(@NotNull TypeMirror type,
                           @NotNull AdapterInfo mapper,
                           @NotNull List<GenericTypeInfo> nestedGeneric,
                           @Nullable ConverterData converter) {
        this.mapper = new SpecifiedMapper(type, mapper, mapper);
        this.nestedGeneric = nestedGeneric;
        this.adapterType = SpecifiedMapper.AdapterType.MAPPER;
        this.converter = converter;
        this.typeResolver = null;
    }

    public GenericTypeInfo(@NotNull TypeMirror type,
                           @Nullable AdapterInfo parser,
                           @Nullable AdapterInfo serializer,
                           @NotNull List<GenericTypeInfo> nestedGeneric,
                           @Nullable ConverterData converter) {
        this.mapper = new SpecifiedMapper(type, parser, serializer);
        this.nestedGeneric = nestedGeneric;
        this.adapterType = mapper.adapterType();
        this.converter = converter;
        this.typeResolver = null;
    }

    public GenericTypeInfo(@NotNull TypeMirror type,
                           @NotNull List<GenericTypeInfo> nestedGeneric,
                           @Nullable ConverterData converter,
                           @NotNull ConcreteTypeResolver typeResolver) {
        this.mapper = new SpecifiedMapper(type, null, null);
        this.nestedGeneric = nestedGeneric;
        this.adapterType = typeResolver.subtypes.values().stream()
                .map(SpecifiedMapper::adapterType)
                .reduce(SpecifiedMapper.AdapterType.MAPPER, (l, r) -> {
                    if (l == r) return l;
                    if (l == SpecifiedMapper.AdapterType.MAPPER) return r;
                    if (r == SpecifiedMapper.AdapterType.MAPPER) return l;
                    throw new IllegalArgumentException("Unsolvable constraints: " + typeResolver.subtypes);
                });
        this.converter = converter;
        this.typeResolver = typeResolver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericTypeInfo that = (GenericTypeInfo) o;
        return mapper.equals(that.mapper) &&
                nestedGeneric.equals(that.nestedGeneric) &&
                adapterType == that.adapterType &&
                Objects.equals(converter, that.converter) &&
                Objects.equals(typeResolver, that.typeResolver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper, nestedGeneric, adapterType, converter, typeResolver);
    }

    @Override
    public String toString() {
        return "GenericTypeInfo{" +
                "mapper=" + mapper +
                ", nestedGeneric=" + nestedGeneric +
                ", adapterType=" + adapterType +
                ", converter=" + converter +
                ", typeResolver=" + typeResolver +
                '}';
    }
}
