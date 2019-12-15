package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import javax.lang.model.type.TypeMirror;

/**
 * Description how to get value
 */
public final class Getter {
    /**
     * Serialized name
     */
    @NotNull
    public final String propertyName;
    /**
     * Name in code
     */
    @NotNull
    public final String getterName;
    /**
     * {@code true} if it is method, {@code false} if it is field access
     */
    public final boolean isMethod;
    @NotNull
    public final AdapterInfo adapter;
    @NotNull
    public final String defaultValue;
    @NotNull
    public final List<@NotNull GenericTypeInfo> genericArguments;
    @Nullable
    public final ConverterData converter;

    public Getter(@NotNull String propertyName,
                  @NotNull String getterName,
                  boolean isMethod,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments,
                  @Nullable ConverterData converter) {
        this.propertyName = propertyName;
        this.getterName = getterName;
        this.isMethod = isMethod;
        this.adapter = adapter;
        this.defaultValue = defaultValue;
        this.genericArguments = genericArguments;
        this.converter = converter;
    }

    public Getter(@NotNull String propertyName,
                  @NotNull String getterName,
                  boolean isMethod,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @NotNull List<@NotNull GenericTypeInfo> genericArguments) {
        this(propertyName, getterName, isMethod, adapter, defaultValue, genericArguments, null);
    }

    public Getter(@NotNull String propertyName,
                  @NotNull String getterName,
                  boolean isMethod,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue,
                  @Nullable ConverterData converter) {
        this(propertyName, getterName, isMethod, adapter, defaultValue, Collections.emptyList(), converter);
    }

    public Getter(@NotNull String propertyName,
                  @NotNull String getterName,
                  boolean isMethod,
                  @NotNull AdapterInfo adapter,
                  @NotNull String defaultValue) {
        this(propertyName, getterName, isMethod, adapter, defaultValue, Collections.emptyList(), null);
    }

    @NotNull
    @Override
    public String toString() {
        return "Getter{" +
                "propertyName='" + propertyName + '\'' +
                ", getterName='" + getterName + '\'' +
                ", isMethod=" + isMethod +
                ", adapter=" + adapter +
                ", defaultValue='" + defaultValue + '\'' +
                ", genericArguments=" + genericArguments +
                ", converter=" + converter +
                '}';
    }
}
