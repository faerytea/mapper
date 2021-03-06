/*
 * Copyright 2020 Valery Maevsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitlab.faerytea.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks properties. Final fields and getter-like methods (i.e. {@code <Type>
 * methodName()} &mdash; no args and non-void return type) creates 'read' properties,
 * setter-like methods (any return type and exactly one argument) and parameters
 * creates 'write' properties, non-final fields creates both. If 'read' property set
 * encloses 'write' property set then serializer will be generated; if 'write' PS
 * encloses 'read' PS then parser will be generated.
 * </p>
 * <p>
 * <b>Note:</b> if used on method parameter one of these must be satisfied:
 * <ul>
 *     <li>All parameters of method are annotated</li>
 *     <li>Annotated parameter is a parameter of constructor, annotated with
 *     {@link Mappable}</li>
 * </ul>
 * </p>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface Property {
    /**
     * Name of this property. Defaults to field name, parameter name or method name without
     * set/get prefix.
     *
     * @return serialized name of this property
     */
    String value() default "";

    /**
     * Use non-default mapper for type specified
     * in {@link #value()}.
     *
     * @return description of mapper
     */
    SpecificMapper via() default @SpecificMapper;
}
