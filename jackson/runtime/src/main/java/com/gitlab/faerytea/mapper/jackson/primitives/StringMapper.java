package com.gitlab.faerytea.mapper.jackson.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;

@DefaultMapper
public class StringMapper implements MappingAdapter<String, JsonParser, JsonGenerator> {
    @Instance
    public static final StringMapper INSTANCE = new StringMapper();

    @Override
    public String toObject(JsonParser source) throws IOException {
        try {
            return source.currentToken() == JsonToken.VALUE_NULL ? null : source.getText();
        } finally {
            source.nextToken();
        }
    }

    @Override
    public void write(String object, JsonGenerator to) throws IOException {
        to.writeString(object);
    }
}
