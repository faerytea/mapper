package com.gitlab.faerytea.mapper.converters;

public interface LongConverter<T> extends MarkerConverter {
    T fromLong(long value);
    long toLong(T value);
}
