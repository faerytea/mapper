package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

public interface SerializerInt<Output> {
    void write(int object, Output to) throws IOException;
}
