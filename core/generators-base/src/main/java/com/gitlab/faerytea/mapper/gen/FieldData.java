package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.lang.model.type.TypeMirror;

/**
 * Info about field.
 */
public final class FieldData {
    /**
     * Name of field in serialized form.
     */
    @NotNull
    public final String fieldName;
    /**
     * Type of field.
     */
    @NotNull
    public final TypeMirror fieldType;
    @NotNull
    public final List<@NotNull Getter> getters;
    @NotNull
    public final List<@NotNull Setter> setters;
    public final boolean required;
    @Nullable
    public final ValidatorInfo validator;

    public FieldData(
            @NotNull String fieldName,
            @NotNull TypeMirror fieldType,
            @NotNull List<@NotNull Getter> getters,
            @NotNull List<@NotNull Setter> setters,
            boolean required,
            @Nullable ValidatorInfo validator) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.getters = getters;
        this.setters = setters;
        this.required = required;
        this.validator = validator;
    }

    @Override
    public String toString() {
        return "FieldData{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType=" + fieldType +
                ", getters=" + getters +
                ", setters=" + setters +
                ", required=" + required +
                ", validator=" + validator +
                '}';
    }
}
