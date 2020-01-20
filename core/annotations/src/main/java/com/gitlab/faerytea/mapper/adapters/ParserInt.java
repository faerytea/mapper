package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

public interface ParserInt<Input> {
    int toObject(Input source) throws IOException;
}
