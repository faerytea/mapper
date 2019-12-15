package com.gitlab.faerytea.mapper.adapters;

public interface Serializer<T, Output> {
    void write(T object, Output to);
}
