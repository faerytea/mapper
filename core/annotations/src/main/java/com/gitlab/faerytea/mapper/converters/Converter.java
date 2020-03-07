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
package com.gitlab.faerytea.mapper.converters;

/**
 * Converter interface which helps convert objects
 * between understandable and useful forms
 *
 * @param <From> understandable form (i.e. mappable)
 * @param <To>   useful form (i.e. type in java)
 */
public interface Converter<From, To> extends MarkerConverter {
    /**
     * Convert {@code value} to useful form
     *
     * @param value mappable form
     * @return useful form
     */
    To decode(From value);

    /**
     * Convert {@code value} to mappable form
     *
     * @param value useful form
     * @return mappable form
     */
    From encode(To value);
}
