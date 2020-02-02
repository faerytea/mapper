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

import com.gitlab.faerytea.mapper.adapters.DoNothing;
import com.gitlab.faerytea.mapper.adapters.UnknownPropertyHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark class as mappable, so code will be generated.
 * Applying to constructor implicitly marks all constructor
 * parameters as {@link Property}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Mappable {
    /**
     * Specify handler for unknown properties.
     *
     * @return handler
     * @see UnknownPropertyHandler
     * @see DoNothing
     */
    Class<? extends UnknownPropertyHandler> onUnknown() default DoNothing.class;

    /**
     * Specify named instance of unknown property handler.
     * Use only with {@link #onUnknown()}
     *
     * @return name of named instance of {@link UnknownPropertyHandler}
     */
    String onUnknownNamed() default "";
}
