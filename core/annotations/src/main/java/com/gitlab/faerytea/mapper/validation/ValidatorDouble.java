package com.gitlab.faerytea.mapper.validation;

public interface ValidatorDouble {
    void validate(String serializedName,
                  String jvmName,
                  Class<?> enclosing,
                  double value) throws IllegalStateException;
}
