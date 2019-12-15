package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class ConverterData {
    @NotNull
    public final AdapterInfo converter;
    @NotNull
    public final TypeMirror from;
    @NotNull
    public final TypeMirror to;

    public ConverterData(@NotNull AdapterInfo converter, @NotNull TypeMirror from, @NotNull TypeMirror to) {
        this.converter = converter;
        this.from = from;
        this.to = to;
    }

    /**
     * Decides which method you should call for converting from
     * {@link #from} to {@link #to}
     *
     * @return forward conversion method name
     */
    @NotNull
    public String decodeName() {
        final TypeKind fromKind = from.getKind();
        final TypeKind toKind = to.getKind();
        if (fromKind == toKind || (!fromKind.isPrimitive() && !toKind.isPrimitive()))
            return "decode";
        if (toKind.isPrimitive()) {
            String x = nameByKind("to", toKind);
            if (x != null) return x;
        } else {
            String x = nameByKind("from", fromKind);
            if (x != null) return x;
        }
        throw new IllegalStateException("from is " + from + ", to is " + to + "; no forward conversion found");
    }

    /**
     * Decides which method you should call for converting from
     * {@link #to} to {@link #from}
     *
     * @return backward conversion method name
     */
    @NotNull
    public String encodeName() {
        final TypeKind fromKind = from.getKind();
        final TypeKind toKind = to.getKind();
        if (fromKind == toKind || (!fromKind.isPrimitive() && !toKind.isPrimitive()))
            return "encode";
        if (fromKind.isPrimitive()) {
            String x = nameByKind("to", fromKind);
            if (x != null) return x;
        } else {
            String x = nameByKind("from", toKind);
            if (x != null) return x;
        }
        throw new IllegalStateException("from is " + from + ", to is " + to + "; no backward conversion found");
    }

    @Nullable
    public static String nameByKind(@NotNull String prefix, @NotNull TypeKind kind) {
        switch (kind) {
            case BYTE:
            case SHORT:
            case INT:
                return prefix + "Int";
            case FLOAT:
            case DOUBLE:
                return prefix + "Double";
            case BOOLEAN:
                return prefix + "Boolean";
            case LONG:
                return prefix + "Long";
        }
        return null;
    }
}
