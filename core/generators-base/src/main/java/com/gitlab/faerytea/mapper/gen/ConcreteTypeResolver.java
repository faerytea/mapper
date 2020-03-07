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

import com.gitlab.faerytea.mapper.annotations.SpecificMapper;
import com.gitlab.faerytea.mapper.polymorph.Subtype;
import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver;
import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver.Variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.util.Types;

public final class ConcreteTypeResolver {
    @NotNull
    public final Variant variant;
    @NotNull
    public final Map<String, SpecifiedMapper> subtypes;
    @NotNull
    public final SpecifiedMapper defaultSubtype;
    @NotNull
    public final String propertyNameClass;
    @NotNull
    public final String propertyNameValue;
    @NotNull
    public final List<GenericTypeInfo> classGenerics;

    public ConcreteTypeResolver(@NotNull Variant variant,
                                @NotNull Map<String, SpecifiedMapper> subtypes,
                                @NotNull SpecifiedMapper defaultSubtype,
                                @NotNull String propertyNameClass,
                                @NotNull String propertyNameValue,
                                @NotNull List<GenericTypeInfo> classGenerics) {
        this.variant = variant;
        this.subtypes = subtypes;
        this.defaultSubtype = defaultSubtype;
        this.propertyNameClass = propertyNameClass;
        this.propertyNameValue = propertyNameValue;
        this.classGenerics = classGenerics;
    }

    public ConcreteTypeResolver(@NotNull ProcessingEnvironment env,
                                @NotNull SubtypeResolver annotation,
                                @NotNull BiFunction<ReferenceType, SpecificMapper, SpecifiedMapper> mapper,
                                @NotNull SpecifiedMapper defaultSubtype,
                                @NotNull List<GenericTypeInfo> classGenerics) {
        this.variant = annotation.variant();
        this.propertyNameClass = annotation.classKey();
        this.propertyNameValue = annotation.valueKey();
        this.defaultSubtype = defaultSubtype;
        this.classGenerics = classGenerics;
        final Subtype[] subtypes = annotation.subtypes();
        final HashMap<String, SpecifiedMapper> map = new HashMap<>();
        for (final Subtype t: subtypes) {
            final String name = t.name();
            final ReferenceType type = mirror(env, t);
            map.put(name.isEmpty() ? type.toString() : name, mapper.apply(type, t.via()));
        }
        this.subtypes = map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcreteTypeResolver that = (ConcreteTypeResolver) o;
        return variant == that.variant &&
                subtypes.equals(that.subtypes) &&
                propertyNameClass.equals(that.propertyNameClass) &&
                propertyNameValue.equals(that.propertyNameValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, subtypes, propertyNameClass, propertyNameValue);
    }

    private static ReferenceType mirror(@NotNull ProcessingEnvironment env, @NotNull Subtype tp) {
        try {
            Class<?> value = tp.value();
            int i = 0;
            while (value.isArray()) {
                ++i;
                value = value.getComponentType();
            }
            final Types typeUtils = env.getTypeUtils();
            ReferenceType res = typeUtils.getDeclaredType(env.getElementUtils().getTypeElement(value.getCanonicalName()));
            while (i != 0) {
                --i;
                res = typeUtils.getArrayType(res);
            }
            return res;
        } catch (final MirroredTypeException e) {
            return (ReferenceType) e.getTypeMirror();
        }
    }
}
