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
package com.gitlab.faerytea.mapper.validation;

/**
 * Check that field is not null.
 * Can not be used on types, only on fields / methods.
 */
public class NonNullValidator implements Validator<Object> {
    @Override
    public void validate(String serializedName, String jvmName, Class<?> enclosing, Class<?> type, Object value) throws IllegalStateException {
        if (value == null)
            throw new IllegalStateException("Field " + serializedName + " is null (in "
                    + enclosing.getCanonicalName() + ", attempt to access via " + jvmName + ")");
    }
}
