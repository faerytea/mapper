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

import java.util.Collection;
import java.util.Collections;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class SimpleGenerator implements Generator {
    protected final Filer filer;
    protected final Types typeUtils;
    protected final Elements elemUtils;
    protected final Messager messager;
    protected final ProcessingEnvironment env;
    protected final ReferenceType inputType;
    protected final ReferenceType outputType;

    protected SimpleGenerator(@NotNull ProcessingEnvironment env,
                              @NotNull CharSequence inputTypeName,
                              @NotNull CharSequence outputTypeName)
            throws GeneratingException {
        this.env = env;
        this.filer = env.getFiler();
        this.typeUtils = env.getTypeUtils();
        this.elemUtils = env.getElementUtils();
        this.messager = env.getMessager();
        try {
            this.inputType = (ReferenceType) elemUtils.getTypeElement(inputTypeName).asType();
            this.outputType = (ReferenceType) elemUtils.getTypeElement(outputTypeName).asType();
        } catch (NullPointerException | ClassCastException e) {
            throw new GeneratingException("Bad Input / Output type's names provided: "
                    + inputTypeName + " and " + outputTypeName
                    + "\nProbably these classes are not in compile classpath?", e);
        }
    }

    @NotNull
    @Override
    public ReferenceType getInputTypeName() {
        return inputType;
    }

    @NotNull
    @Override
    public ReferenceType getOutputTypeName() {
        return outputType;
    }

    @NotNull
    @Override
    public Collection<@NotNull InstanceData> getInstances() {
        return Collections.emptySet();
    }

    /**
     * Package of type or empty string for top-level class
     *
     * @param targetType type
     * @return package in form of string
     */
    @NotNull
    protected String packageOf(@NotNull TypeElement targetType) {
        return elemUtils.getPackageOf(targetType).getQualifiedName().toString();
    }

    /**
     * Convenience method for converting class name to {@link Element}.
     *
     * @param name name of class / interface
     * @return corresponding TypeElement
     */
    @NotNull
    protected TypeElement typeElementFrom(@NotNull CharSequence name) {
        return elemUtils.getTypeElement(name);
    }

    /**
     * Convenience method for converting class name to {@link TypeMirror}.
     *
     * @param name name of class / interface
     * @return corresponding DeclaredType
     */
    @NotNull
    protected DeclaredType typeFrom(@NotNull CharSequence name) {
        return typeUtils.getDeclaredType(typeElementFrom(name));
    }

    /**
     * Convenience method for converting {@link Class} to {@link Element}.
     *
     * @param clazz class / interface object
     * @return corresponding TypeElement
     */
    @NotNull
    protected TypeElement typeElementFrom(@NotNull Class<?> clazz) {
        return elemUtils.getTypeElement(clazz.getCanonicalName());
    }

    /**
     * Convenience method for converting {@link Class} to {@link TypeMirror}.
     *
     * @param clazz class / interface object
     * @return corresponding DeclaredType
     */
    @NotNull
    protected DeclaredType typeFrom(@NotNull Class<?> clazz) {
        return typeUtils.getDeclaredType(typeElementFrom(clazz));
    }
}
