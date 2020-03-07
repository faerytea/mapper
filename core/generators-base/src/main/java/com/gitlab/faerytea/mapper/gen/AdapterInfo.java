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

public final class AdapterInfo {
    /**
     * Fully qualified adapter name
     */
    @NotNull
    public final String className;
    /**
     * Way to obtain instance of {@link #className}
     */
    @Nullable
    public final InstanceData instance;

    public AdapterInfo(@NotNull CharSequence className) {
        this(className, null);
    }

    public AdapterInfo(@NotNull CharSequence className, @Nullable InstanceData instance) {
        this.className = className.toString();
        this.instance = instance;
    }

    @NotNull
    @Override
    public String toString() {
        return "AdapterInfo{" +
                "className=" + className +
                ", instance=" + instance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdapterInfo that = (AdapterInfo) o;
        return className.equals(that.className) &&
                Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, instance);
    }

}
