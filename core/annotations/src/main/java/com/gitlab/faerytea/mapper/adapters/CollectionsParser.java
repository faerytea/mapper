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
 * Parsers for generic collections.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this parser.
 * <br/>
 * Note that type parameter {@code C} will be
 * checked by processor for every
 * {@linkplain com.gitlab.faerytea.mapper.annotations.DefaultParser DefaultParser}
 * subclass for restrictions noted in methods' documentation
 *
 * @param <C>     raw type of collection
 * @param <Input> input type
 */
@SuppressWarnings("rawtypes")
public interface CollectionsParser<C extends Iterable, Input> {
    /**
     * Main parser method. For {@code @DefaultParser}s
     * processor will check that return type is actually
     * {@code C<T>}.
     *
     * @param source     serialized data
     * @param itemParser parser for items
     * @param <T>        type of items
     * @return properly typed collection
     */
    <T> C toObject(Input source, Parser<T, Input> itemParser) throws IOException;

    /**
     * Way to create parser. For {@code @DefaultParser}s
     * processor will check that return type is actually
     * {@code Parser<C<T>, Input>}.
     *
     * @param itemParser parser for items
     * @param <T>        type of items
     * @return parser for properly typed collection
     */
    <T> Parser<? extends C, Input> apply(Parser<T, Input> itemParser);
}
