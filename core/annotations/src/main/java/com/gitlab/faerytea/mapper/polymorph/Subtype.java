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
package com.gitlab.faerytea.mapper.polymorph;

import com.gitlab.faerytea.mapper.annotations.SpecificMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines subtype.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Subtype {
    /**
     * Concrete subtype of property's type.
     * Will be checked during serialization / deserialization.
     *
     * @return concrete subclass
     */
    Class<?> value();

    /**
     * Alias for class. Defaults to canonical
     * class name.
     *
     * @return subtype identifier
     */
    String name() default "";

    /**
     * Use non-default mapper for type specified
     * in {@link #value()}.
     *
     * @return description of mapper
     */
    SpecificMapper via() default @SpecificMapper;
}
