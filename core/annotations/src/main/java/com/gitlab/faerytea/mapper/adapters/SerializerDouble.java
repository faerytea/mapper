package com.gitlab.faerytea.mapper.adapters;

public interface SerializerDouble<Output> {
    void write(double value, Output to);
}
