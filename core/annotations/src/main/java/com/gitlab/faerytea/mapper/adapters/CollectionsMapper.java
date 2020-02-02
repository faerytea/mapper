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

/**
 * Mappers for generic collections.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this mapper.
 * <br/>
 * Note that type parameter {@code C} will be
 * checked by processor for every
 * {@linkplain com.gitlab.faerytea.mapper.annotations.DefaultMapper DefaultMapper}
 * subclass for restrictions noted in methods' documentation
 *
 * @param <C>     raw type of collection
 * @param <Input> input type
 */
public interface CollectionsMapper<C extends Iterable, Input, Output> extends CollectionsParser<C, Input>, CollectionsSerializer<C, Output> {
    /**
     * Way to create mapper. For {@code @DefaultMapper}s
     * processor will check that return type is actually
     * {@code MappingAdapter<C<T>, Input>}.
     *
     * @param itemMapper mapper for items
     * @param <T>        type of items
     * @return mapper for properly typed collection
     */
    <T> MappingAdapter<? extends C, Input, Output> apply(MappingAdapter<T, Input, Output> itemMapper);
}
