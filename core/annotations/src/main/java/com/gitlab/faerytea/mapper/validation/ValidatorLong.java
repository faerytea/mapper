package com.gitlab.faerytea.mapper.validation;

public interface ValidatorLong {
    void validate(String serializedName,
                  String jvmName,
                  Class<?> enclosing,
                  long value) throws IllegalStateException;
}
