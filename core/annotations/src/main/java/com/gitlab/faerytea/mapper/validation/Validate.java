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
package com.gitlab.faerytea.mapper.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface Validate {
    /**
     * Provide code which will be used for validation.
     * You can use following placeholders:
     * <ul>
     *     <li>{@code $t} for field type on jvm</li>
     *     <li>{@code $e} for enclosing type</li>
     *     <li>{@code $s} for serialized field name</li>
     *     <li>{@code $j} for JVM field name or setter name</li>
     *     <li>{@code $v} for value</li>
     * </ul>
     * Example:
     * {@code "if ($v == null) throw new IllegalStateException(\"$s in $e is null\");"}
     *
     * @return java code
     */
    String value() default "";
    Class<? extends Validator> validator() default Validator.class;
}
