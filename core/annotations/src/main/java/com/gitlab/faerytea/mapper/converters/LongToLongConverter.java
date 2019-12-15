package com.gitlab.faerytea.mapper.converters;

public interface LongToLongConverter extends MarkerConverter {
    long encode(long value);
    long decode(long value);
}
