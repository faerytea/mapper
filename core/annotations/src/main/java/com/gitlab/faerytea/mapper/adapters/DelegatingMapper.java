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
 * Mapper which delegates both operations to another parser
 * and serializer
 *
 * {@inheritDoc}
 */
public class DelegatingMapper<T, Input, Output> implements MappingAdapter<T, Input, Output> {
    public final Parser<T, Input> parser;
    public final Serializer<T, Output> serializer;

    /**
     * Combines parser and adapter into a single mapper
     *
     * @param parser     which will parse
     * @param serializer which will serialize
     */
    public DelegatingMapper(Parser<T, Input> parser, Serializer<T, Output> serializer) {
        this.parser = parser;
        this.serializer = serializer;
    }

    @Override
    public T toObject(Input source) throws IOException {
        return parser.toObject(source);
    }

    @Override
    public void write(T object, Output to) throws IOException {
        serializer.write(object, to);
    }
}
