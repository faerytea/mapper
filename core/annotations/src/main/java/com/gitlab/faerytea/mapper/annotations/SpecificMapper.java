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

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use non-standard mappers for {@link Property}.
 * Can only be used in conjunction with other annotations
 * as an argument in {@link Property#via()} and
 * {@link PutOnTypeArguments.OnArg#via()}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface SpecificMapper {
    /**
     * Specify {@link MappingAdapter} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}. Overrides {@link #serializeUsing()}
     * and {@link #parseUsing()}.
     *
     * @return adapter
     */
    Class<?> using() default MappingAdapter.class;

    /**
     * Specify {@link Serializer} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}.
     *
     * @return adapter
     */
    Class<?> serializeUsing() default Serializer.class;

    /**
     * Specify {@link Parser} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}.
     *
     * @return adapter
     */
    Class<?> parseUsing() default Parser.class;

    /**
     * Specify {@link MappingAdapter} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}. Overrides {@link #serializeUsingNamed()}
     * and {@link #parseUsingNamed()}.
     *
     * @return adapter
     */
    String usingNamed() default "";

    /**
     * Specify {@link Serializer} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}.
     *
     * @return adapter
     */
    String serializeUsingNamed() default "";

    /**
     * Specify {@link Parser} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}.
     *
     * @return adapter
     */
    String parseUsingNamed() default "";
}
