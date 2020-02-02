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
package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

/**
 * Serializers for generic collections.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this serializer.
 * <br/>
 * Note that type parameter {@code C} will be
 * checked by processor for every
 * {@linkplain com.gitlab.faerytea.mapper.annotations.DefaultSerializer DefaultSerializer}
 * subclass for restrictions noted in methods' documentation
 *
 * @param <C>      raw type of collection
 * @param <Output> output type
 */
@SuppressWarnings("rawtypes")
public interface CollectionsSerializer<C extends Iterable, Output> {
    /**
     * Main serializer method. Due to limitations of
     * java type system {@code object}'s type remains raw.
     *
     * @param object         what to serialize
     * @param itemSerializer serializer for items
     * @param to             serialized data
     * @param <T>            type of items
     */
    <T> void write(C object, Output to, Serializer<T, Output> itemSerializer) throws IOException;

    /**
     * Way to create serializer. For {@code @DefaultSerializer}s
     * processor will check that return type is actually
     * {@code Serializer<C<T>, Output>}.
     *
     * @param itemSerializer serializer for items
     * @param <T>            type of items
     * @return serializer for properly typed collection
     */
    <T> Serializer<? extends C, Output> apply(Serializer<T, Output> itemSerializer);
}
