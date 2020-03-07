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

import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Abstract class, representing primitives, arrays and
 * declared types.
 */
public abstract class TypeInfo {
    @NotNull
    public static TypeInfo from(@NotNull TypeMirror tp) {
        switch (tp.getKind()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return new Primitive(((PrimitiveType) tp));
            case ARRAY:
                return new Array((ArrayType) tp);
            case DECLARED:
            case ERROR:
                return new Declared(((DeclaredType) tp));
        }
        throw new IllegalArgumentException("type " + tp + " is not supported");
    }

    @NotNull
    public static TypeInfo from(@NotNull Element element) {
        return from(element.asType());
    }

    @NotNull
    public final TypeMirror builtBy;

    private TypeInfo(@NotNull TypeMirror builtBy) {
        this.builtBy = builtBy;
    }

    private static final class Primitive extends TypeInfo {
        private final TypeKind kind;

        protected Primitive(@NotNull PrimitiveType builtBy) {
            super(builtBy);
            kind = builtBy.getKind();
            assert kind.isPrimitive();
        }

        @Override
        public int hashCode() {
            return kind.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == Primitive.class && kind == ((Primitive) obj).kind;
        }
    }

    private static final class Declared extends TypeInfo {
        private final String fqName;

        protected Declared(@NotNull DeclaredType builtBy) {
            super(builtBy);
            fqName = builtBy.toString(); // for declared type should be FQ name
        }

        @Override
        public int hashCode() {
            return fqName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == Declared.class && fqName.equals(((Declared) obj).fqName);
        }
    }

    private static final class Array extends TypeInfo {
        private final TypeInfo component;

        protected Array(@NotNull ArrayType builtBy) {
            super(builtBy);
            component = from(builtBy.getComponentType());
        }

        @Override
        public int hashCode() {
            final int hashCode = component.hashCode();
            return (hashCode << 1) + (hashCode & 1);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == Array.class && component.equals(((Array) obj).component);
        }
    }
}
