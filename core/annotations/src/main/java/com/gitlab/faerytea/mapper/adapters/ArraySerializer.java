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
 * Serializers for Java's arrays.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this serializer
 *
 * @param <Output> output type
 */
public interface ArraySerializer<Output> {
    <ComponentTp> void write(ComponentTp[] object, Output to, Serializer<ComponentTp, Output> componentSerializer) throws IOException;
    <ComponentTp> Serializer<ComponentTp[], Output> apply(Serializer<ComponentTp, Output> componentSerializer);
}
