package com.gitlab.faerytea.mapper.validation;

public interface ValidatorInt {
    void validate(String serializedName,
                  String jvmName,
                  Class<?> enclosing,
                  int value) throws IllegalStateException;
}
