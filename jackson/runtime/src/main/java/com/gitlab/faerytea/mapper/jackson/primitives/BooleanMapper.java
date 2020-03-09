package com.gitlab.faerytea.mapper.jackson.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.gitlab.faerytea.mapper.adapters.MappingAdapterBoolean;
import com.gitlab.faerytea.mapper.adapters.MappingAdapterInt;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;

@DefaultMapper
public class BooleanMapper implements MappingAdapterBoolean<JsonParser, JsonGenerator> {
    @Instance
    public static final BooleanMapper INSTANCE = new BooleanMapper();

    @Override
    public void write(boolean object, JsonGenerator to) throws IOException {
        to.writeBoolean(object);
    }

    @Override
    public boolean toObject(JsonParser source) throws IOException {
        try {
            return source.getBooleanValue();
        } finally {
            source.nextToken();
        }
    }
}
