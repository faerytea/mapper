package com.gitlab.faerytea.mapper.converters;

public interface IntToIntConverter extends MarkerConverter {
    int decode(int value);
    int encode(int value);
}
