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
 * Adapter which always throws.
 */
@SuppressWarnings("rawtypes")
public class ThrowingAdapter implements MappingAdapter {
    @Override
    public Object toObject(Object source) throws IOException {
        throw new IllegalArgumentException("cannot extract anything from " + source);
    }

    @Override
    public void write(Object object, Object to) throws IOException {
        throw new IllegalArgumentException(object.getClass().getCanonicalName() + " cannot be serialized");
    }
}
