package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

public interface Parser<T, Input> {
    T toObject(Input source) throws IOException;
}
