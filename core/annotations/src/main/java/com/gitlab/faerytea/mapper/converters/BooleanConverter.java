package com.gitlab.faerytea.mapper.converters;

public interface BooleanConverter<T> extends MarkerConverter {
    boolean toBool(T value);
    T fromBool(boolean value);
}
