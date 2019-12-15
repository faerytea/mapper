package com.gitlab.faerytea.mapper.converters;

public interface BooleanIntConverter extends MarkerConverter {
    boolean toBoolean(int value);
    int toInt(boolean value);
}
