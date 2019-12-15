package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import javax.lang.model.type.TypeMirror;

public class GenericTypeInfo {
    @NotNull
    public final TypeMirror type;
    @NotNull
    public final AdapterInfo adapter;
    @NotNull
    public final List<GenericTypeInfo> nestedGeneric;
    @NotNull
    public final AdapterType adapterType;

    public GenericTypeInfo(@NotNull TypeMirror type, @NotNull AdapterInfo adapter, @NotNull List<GenericTypeInfo> nestedGeneric, @NotNull AdapterType adapterType) {
        this.type = type;
        this.adapter = adapter;
        this.nestedGeneric = nestedGeneric;
        this.adapterType = adapterType;
    }

    @Override
    public String toString() {
        return "GenericTypeInfo{" +
                "type=" + type +
                ", adapter=" + adapter +
                ", nestedGeneric=" + nestedGeneric +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericTypeInfo that = (GenericTypeInfo) o;
        return type.equals(that.type) &&
                adapter.equals(that.adapter) &&
                nestedGeneric.equals(that.nestedGeneric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, adapter, nestedGeneric);
    }

    public enum AdapterType {
        PARSER, SERIALIZER, MAPPER
    }
}
