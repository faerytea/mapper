package com.gitlab.faerytea.mapper.converters;

public interface DoubleLongConverter extends MarkerConverter {
    double toDouble(long value);
    long toLong(double value);
}
