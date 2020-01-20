package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

public interface SerializerDouble<Output> {
    void write(double value, Output to) throws IOException;
}
