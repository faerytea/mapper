package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class AdapterInfo {
    /**
     * Fully qualified adapter name
     */
    @NotNull
    public final String  className;
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
