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

import com.gitlab.faerytea.mapper.converters.Convert;
import com.gitlab.faerytea.mapper.converters.Converter;
import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Since Java annotation processors have troubles
 * getting annotations on type arguments (source level 8)
 * or these annotation not available at all
 * (before java 8) there is this annotation provided
 * as a hack.
 * <p>
 * Put this annotation on field, method or parameter
 * with generic type and specific annotations will
 * work just like you put them on field of n'th type.
 * <p>
 * Example:
 * <pre>
 *     &#64;PutOnTypeArguments(&#64;OnArg(convert = &#64;Convert(StringNameConverter.class)))
 *     List&lt;Name&gt; users;
 *
 * StringNameConverter.java:
 *
 *     public class StringNameConverter implements Converter&lt;String, Name&gt; {
 *         public Name decode(String value) {
 *             String[] names = value.split(" +");
 *             return new Name(names[0], names[1]);
 *         }
 *
 *         public String encode(Name value) {
 *             return value.firstName + " " value.lastName;
 *         }
 *     }
 *
 * Name.java:
 *
 *     public class Name {
 *         public final String firstName;
 *         public final String lastName;
 *
 *         public Name(String firstName, String lastName) {
 *             this.firstName = firstName;
 *             this.lastName = lastName;
 *         }
 *     }
 * </pre>
 * Annotation's argument at <i>n</i>'th position refers to
 * type argument at <i>n</i>'th position. For skipping
 * arbitrary position you can place {@code @OnArg(false)}.
 * Array must have same length as sum of parameters lists.
 * As example: {@code Map<String, List<Name>>} corresponds
 * to [on String, on List&lt;Name&gt;, on Name] — you can
 * think about it as sequence from tree iterator.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface PutOnTypeArguments {
    OnArg[] value();

    @Retention(RetentionPolicy.SOURCE)
    @Target({})
    @interface OnArg {
        /**
         * Is this element informative?
         * Default is true and can be omitted, but passing {@code false}
         * explicitly marks this annotation as placeholder and force
         * processor to skip it.
         *
         * @return {@code true} if it should be parsed, {@code false} otherwise.
         */
        boolean value() default true;

        /**
         * Specify converter
         *
         * @return {@code Convert} annotation
         */
        Convert convert() default @Convert(Converter.class);

        /**
         * Use non-default mapper for type on this
         * position.
         *
         * @return description of mapper
         */
        SpecificMapper via() default @SpecificMapper();

        /**
         * Specify resolver
         *
         * @return {@code SubtypeResolver} annotation
         */
        SubtypeResolver resolver() default @SubtypeResolver(variant = SubtypeResolver.Variant.WRAPPER_KEY, subtypes = {});
    }

}
