package com.gitlab.faerytea.mapper.adapters;

public interface SerializerInt<Output> {
    void write(int object, Output to);
}
