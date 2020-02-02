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

import com.gitlab.faerytea.mapper.annotations.Instance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InstanceData {
    /**
     * Fully qualified classname of instance holder
     */
    @NotNull
    public final String holderClassName;
    
    /**
     * Accessor for instance, getter or field name
     */
    @NotNull
    public final String instanceJavaName;

    /**
     * Name of concrete instance
     * @see Instance#value()
     */
    @Nullable
    public final String namedInstanceName;

    /**
     * Is a method?
     */
    public final boolean isMethod;

    public InstanceData(@NotNull CharSequence holderClassName, @NotNull String instanceJavaName, boolean isMethod) {
        this(holderClassName, instanceJavaName, null, isMethod);
    }

    public InstanceData(@NotNull CharSequence holderClassName, @NotNull String instanceJavaName, @Nullable String namedInstanceName, boolean isMethod) {
        this.holderClassName = holderClassName.toString();
        this.instanceJavaName = instanceJavaName;
        this.namedInstanceName = namedInstanceName;
        this.isMethod = isMethod;
    }

    /**
     * Builds a correct java expression which evaluates to
     * instance of adapter {@code className}.
     *
     * @return accessor for adapter
     */
    @NotNull
    public String javaAccessor() {
        return holderClassName + "." + instanceJavaName + (isMethod ? "()" : "");
    }
}
