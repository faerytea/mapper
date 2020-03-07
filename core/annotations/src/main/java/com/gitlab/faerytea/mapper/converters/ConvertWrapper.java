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

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;

import java.io.IOException;

/**
 * Bunch of useful wrappers for code generation
 */
public final class ConvertWrapper {
    private ConvertWrapper() {}

    /**
     * Combines parser and converter to single parser
     *
     * @param <Result>       desired value
     * @param <Intermediate> supported value
     * @param <Input>        input
     */
    public static class AsParser<Result, Intermediate, Input> implements Parser<Result, Input> {
        private final Parser<Intermediate, Input> delegate;
        private final Converter<Intermediate, Result> converter;

        public AsParser(Parser<Intermediate, Input> delegate, Converter<Intermediate, Result> converter) {
            this.delegate = delegate;
            this.converter = converter;
        }

        @Override
        public Result toObject(Input source) throws IOException {
            return converter.decode(delegate.toObject(source));
        }
    }

    /**
     * Combines serializer and converter to single serializer
     *
     * @param <Input>        available value
     * @param <Intermediate> supported value
     * @param <Output>       output
     */
    public static class AsSerializer<Input, Intermediate, Output> implements Serializer<Input, Output> {
        private final Serializer<Intermediate, Output> delegate;
        private final Converter<Intermediate, Input> converter;

        public AsSerializer(Serializer<Intermediate, Output> delegate, Converter<Intermediate, Input> converter) {
            this.delegate = delegate;
            this.converter = converter;
        }

        @Override
        public void write(Input object, Output to) throws IOException {
            delegate.write(converter.encode(object), to);
        }
    }

    /**
     * Combines mapper and converter to single mapper
     *
     * @param <T>            mapped value
     * @param <Intermediate> supported value
     * @param <Input>        input
     * @param <Output>       output
     */
    public static class AsMapper<T, Intermediate, Input, Output> implements MappingAdapter<T, Input, Output> {
        private final Parser<Intermediate, Input> parser;
        private final Serializer<Intermediate, Output> serializer;
        private final Converter<Intermediate, T> converter;

        public AsMapper(Parser<Intermediate, Input> parser,
                        Serializer<Intermediate, Output> serializer,
                        Converter<Intermediate, T> converter) {
            this.parser = parser;
            this.serializer = serializer;
            this.converter = converter;
        }

        public AsMapper(MappingAdapter<Intermediate, Input, Output> mapper,
                        Converter<Intermediate, T> converter) {
            this(mapper, mapper, converter);
        }

        @Override
        public T toObject(Input source) throws IOException {
            return converter.decode(parser.toObject(source));
        }

        @Override
        public void write(T object, Output to) throws IOException {
            serializer.write(converter.encode(object), to);
        }
    }
}
