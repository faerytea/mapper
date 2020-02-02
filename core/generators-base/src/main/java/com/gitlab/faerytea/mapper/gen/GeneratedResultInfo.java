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

import java.util.Objects;

public final class GeneratedResultInfo {
    @NotNull
    public final AdapterInfo adapter;
    public final boolean canParse;
    public final boolean canSerialize;

    public GeneratedResultInfo(@NotNull AdapterInfo adapter, boolean canParse, boolean canSerialize) {
        this.adapter = adapter;
        this.canParse = canParse;
        this.canSerialize = canSerialize;
    }

    @NotNull
    @Override
    public String toString() {
        return "GeneratedResultInfo{" +
                "adapter=" + adapter +
                ", canParse=" + canParse +
                ", canSerialize=" + canSerialize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratedResultInfo that = (GeneratedResultInfo) o;
        return canParse == that.canParse &&
                canSerialize == that.canSerialize &&
                adapter.equals(that.adapter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adapter, canParse, canSerialize);
    }
}
