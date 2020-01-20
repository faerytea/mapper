package com.gitlab.faerytea.mapper.validation;

public interface Validator<T> {
    void validate(String serializedName,
                  String jvmName,
                  Class<?> enclosing,
                  Class<? extends T> type,
                  T value) throws IllegalStateException;
}
