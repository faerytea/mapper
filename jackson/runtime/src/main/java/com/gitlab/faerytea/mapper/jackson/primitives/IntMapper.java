package com.gitlab.faerytea.mapper.jackson.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.gitlab.faerytea.mapper.adapters.MappingAdapterInt;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;

@DefaultMapper
public class IntMapper implements MappingAdapterInt<JsonParser, JsonGenerator> {
    @Instance
    public static final IntMapper INSTANCE = new IntMapper();

    @Override
    public void write(int object, JsonGenerator to) throws IOException {
        to.writeNumber(object);
    }

    @Override
    public int toObject(JsonParser source) throws IOException {
        try {
            return source.getIntValue();
        } finally {
            source.nextToken();
        }
    }
}
