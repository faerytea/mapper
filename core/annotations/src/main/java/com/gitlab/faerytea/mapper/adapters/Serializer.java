package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

public interface Serializer<T, Output> {
    void write(T object, Output to) throws IOException;
}
