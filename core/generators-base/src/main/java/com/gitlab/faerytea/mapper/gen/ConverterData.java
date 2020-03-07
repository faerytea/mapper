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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class ConverterData {
    @NotNull
    public final AdapterInfo converter;
    /**
     * Understandable form (i.e. mappable)
     */
    @NotNull
    public final TypeMirror from;
    /**
     * Useful form (i.e. type in POJO)
     */
    @NotNull
    public final TypeMirror to;

    public ConverterData(@NotNull AdapterInfo converter, @NotNull TypeMirror from, @NotNull TypeMirror to) {
        this.converter = converter;
        this.from = from;
        this.to = to;
    }

    /**
     * Decides which method you should call for converting from
     * {@link #from} to {@link #to}
     *
     * @return forward conversion method name
     */
    @NotNull
    public String decodeName() {
        final TypeKind fromKind = from.getKind();
        final TypeKind toKind = to.getKind();
        if (fromKind == toKind || (!fromKind.isPrimitive() && !toKind.isPrimitive()))
            return "decode";
        if (toKind.isPrimitive()) {
            String x = nameByKind("to", toKind);
            if (x != null) return x;
        } else {
            String x = nameByKind("from", fromKind);
            if (x != null) return x;
        }
        throw new IllegalStateException("from is " + from + ", to is " + to + "; no forward conversion found");
    }

    /**
     * Decides which method you should call for converting from
     * {@link #to} to {@link #from}
     *
     * @return backward conversion method name
     */
    @NotNull
    public String encodeName() {
        final TypeKind fromKind = from.getKind();
        final TypeKind toKind = to.getKind();
        if (fromKind == toKind || (!fromKind.isPrimitive() && !toKind.isPrimitive()))
            return "encode";
        if (fromKind.isPrimitive()) {
            String x = nameByKind("to", fromKind);
            if (x != null) return x;
        } else {
            String x = nameByKind("from", toKind);
            if (x != null) return x;
        }
        throw new IllegalStateException("from is " + from + ", to is " + to + "; no backward conversion found");
    }

    @Nullable
    public static String nameByKind(@NotNull String prefix, @NotNull TypeKind kind) {
        switch (kind) {
            case BYTE:
            case SHORT:
            case INT:
                return prefix + "Int";
            case FLOAT:
            case DOUBLE:
                return prefix + "Double";
            case BOOLEAN:
                return prefix + "Boolean";
            case LONG:
                return prefix + "Long";
        }
        return null;
    }
}
