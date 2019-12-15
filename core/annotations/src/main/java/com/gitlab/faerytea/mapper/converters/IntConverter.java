package com.gitlab.faerytea.mapper.converters;

public interface IntConverter<T> extends MarkerConverter {
    int toInt(T value);
    T fromInt(int value);
}
