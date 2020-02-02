/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Generators.
 *
 * Mapper Generators is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Generators is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Mapper Generators.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface ValidatorInfo {
    class ValidatorClass implements ValidatorInfo {
        @NotNull
        public final String validatorClass;
        @Nullable
        public final InstanceData instance;

        public ValidatorClass(@NotNull String validatorClass, @Nullable InstanceData instance) {
            this.validatorClass = validatorClass;
            this.instance = instance;
        }

        @NotNull
        @Override
        public String javaStatement(@NotNull String serializedName,
                                    @NotNull String jvmName,
                                    @NotNull String enclosing,
                                    @NotNull String type,
                                    @NotNull String value,
                                    @Nullable String instanceName) {
            if (instanceName == null) {
                if (instance != null) {
                    instanceName = instance.javaAccessor();
                } else {
                    throw new IllegalStateException("both instance and instance data are null");
                }
            }
            final String args;
            switch (type) {
                case "int":
                case "double":
                case "float":
                case "long":
                case "short":
                case "byte":
                    args = ".validate(\"$s\", \"$j\", $e.class, $v);";
                    break;
                default:
                    args = ".validate(\"$s\", \"$j\", $e.class, $t.class, $v);";
                    break;
            }
            return interpolate(instanceName + args, serializedName, jvmName, enclosing, type, value);
        }

        public AdapterInfo asAdapterInfo() {
            return new AdapterInfo(validatorClass, instance);
        }

        @Override
        public String toString() {
            return "ValidatorClass{" +
                    "validatorClass=" + validatorClass +
                    ", instance=" + instance +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidatorClass that = (ValidatorClass) o;
            return validatorClass.equals(that.validatorClass) &&
                    Objects.equals(instance, that.instance);
        }

        @Override
        public int hashCode() {
            return Objects.hash(validatorClass, instance);
        }
    }

    class ValidatorString implements ValidatorInfo {
        @NotNull
        public final String code;

        public ValidatorString(@NotNull String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "ValidatorString{" +
                    "code='" + code + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidatorString that = (ValidatorString) o;
            return code.equals(that.code);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code);
        }

        @Override
        public @NotNull String javaStatement(@NotNull String serializedName,
                                             @NotNull String jvmName,
                                             @NotNull String enclosing,
                                             @NotNull String type,
                                             @NotNull String value,
                                             @Nullable String instanceName) {
            return interpolate(code, serializedName, jvmName, enclosing, type, value);
        }
    }

    @NotNull
    String javaStatement(@NotNull String serializedName,
                         @NotNull String jvmName,
                         @NotNull String enclosing,
                         @NotNull String type,
                         @NotNull String value,
                         @Nullable String instanceName);

    @NotNull
    static String interpolate(@NotNull String original,
                              @NotNull String serializedName,
                              @NotNull String jvmName,
                              @NotNull String enclosing,
                              @NotNull String type,
                              @NotNull String value) {
        final StringBuilder res = new StringBuilder();
        final int length = original.length();
        for (int i = 0; i < length; ++i) {
            final int interI = original.indexOf('$', i);
            if (interI == -1) {
                res.append(original, i, length);
                break;
            } else {
                res.append(original, i, interI);
                if (interI + 1 < length) {
                    final char spec = original.charAt(interI + 1);
                    switch (spec) {
                        case 'j':
                            res.append(jvmName);
                            break;
                        case 'v':
                            res.append(value);
                            break;
                        case 's':
                            res.append(serializedName);
                            break;
                        case 't':
                            res.append(type);
                            break;
                        case 'e':
                            res.append(enclosing);
                            break;
                        default:
                            res.append('$').append(spec);
                            break;
                    }
                    i = interI + 1;
                } else {
                    res.append('$');
                    break;
                }
            }
        }
        return res.toString();
    }
}
