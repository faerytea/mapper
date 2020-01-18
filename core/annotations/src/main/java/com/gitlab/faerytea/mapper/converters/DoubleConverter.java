package com.gitlab.faerytea.mapper.converters;

public interface DoubleConverter<T> extends MarkerConverter {
    T fromDouble(double value);
    double toDouble(T value);
}
