package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

public interface ParserDouble<Input> {
    double toObject(Input source) throws IOException;
}
