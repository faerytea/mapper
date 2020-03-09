package com.gitlab.faerytea.mapper.jackson.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.gitlab.faerytea.mapper.adapters.MappingAdapterLong;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;

@DefaultMapper
public class LongMapper implements MappingAdapterLong<JsonParser, JsonGenerator> {
    @Instance
    public static final LongMapper INSTANCE = new LongMapper();

    @Override
    public void write(long object, JsonGenerator to) throws IOException {
        to.writeNumber(object);
    }

    @Override
    public long toObject(JsonParser source) throws IOException {
        try {
            return source.getLongValue();
        } finally {
            source.nextToken();
        }
    }
}
