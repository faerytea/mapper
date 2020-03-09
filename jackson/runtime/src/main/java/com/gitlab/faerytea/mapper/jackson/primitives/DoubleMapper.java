package com.gitlab.faerytea.mapper.jackson.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.gitlab.faerytea.mapper.adapters.MappingAdapterDouble;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;

@DefaultMapper
public class DoubleMapper implements MappingAdapterDouble<JsonParser, JsonGenerator> {
    @Instance
    public static final DoubleMapper INSTANCE = new DoubleMapper();

    @Override
    public void write(double value, JsonGenerator to) throws IOException {
        to.writeNumber(value);
    }

    @Override
    public double toObject(JsonParser source) throws IOException {
        try {
            return source.getDoubleValue();
        } finally {
            source.nextToken();
        }
    }
}
