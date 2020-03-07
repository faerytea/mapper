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

import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Same as {@link Mappable}, but instructs to generate delegating
 * adapter, which maps subclasses of marked class or implementations
 * of marked interface.
 *
 * @see SubtypeResolver
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface MappableViaSubclasses {
    /**
     * Subtypes configuration
     *
     * @return config
     */
    SubtypeResolver value();

    /**
     * Put {@link DefaultMapper} on generated adapter
     *
     * @return mark generated adapter as default
     */
    boolean markAsDefault() default true;
}
