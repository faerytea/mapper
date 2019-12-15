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
