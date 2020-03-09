package com.gitlab.faerytea.mapper.jackson.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonToken.*;

@DefaultMapper
public class JsonObjectMapper {
    @Instance
    public static final JsonObjectMapper INSTANCE = new JsonObjectMapper();

    public <V> Map<String, V> toObject(JsonParser in, Parser<V, JsonParser> itemParser) throws IOException {
        switch (in.currentToken()) {
            case VALUE_NULL:
                in.nextToken();
                return null;
            case START_OBJECT:
                break;
            default:
                throw new JsonParseException(in, "Expected " + START_OBJECT + ", got " + in.currentToken());
        }
        if (in.nextToken() == END_OBJECT) return Collections.emptyMap();
        HashMap<String, V> res = new HashMap<String, V>();
        do {
            final String key = in.currentName();
            in.nextToken();
            final V value = itemParser.toObject(in);
            res.put(key, value);
        } while (in.currentToken() == FIELD_NAME);
        if (in.currentToken() == END_OBJECT) {
            in.nextToken();
            return res;
        }
        throw new JsonParseException(in, "Expected " + END_OBJECT + ", got " + in.currentToken());
    }

    public <V> void write(Map<String, V> object, JsonGenerator to, Serializer<V, JsonGenerator> itemSerializer) throws IOException {
        if (object == null) {
            to.writeNull();
            return;
        }
        to.writeStartObject();
        for (Map.Entry<String, V> e : object.entrySet()) {
            to.writeFieldName(e.getKey());
            itemSerializer.write(e.getValue(), to);
        }
        to.writeEndObject();
    }

    public <V> Parser<Map<String, V>, JsonParser> apply(final Parser<V, JsonParser> itemParser) {
        return new Parser<Map<String, V>, JsonParser>() {
            @Override
            public Map<String, V> toObject(JsonParser source) throws IOException {
                return JsonObjectMapper.this.toObject(source, itemParser);
            }
        };
    }

    public <V> Serializer<Map<String, V>, JsonGenerator> apply(final Serializer<V, JsonGenerator> itemSerializer) {
        return new Serializer<Map<String, V>, JsonGenerator>() {
            @Override
            public void write(Map<String, V> object, JsonGenerator to) throws IOException {
                JsonObjectMapper.this.write(object, to, itemSerializer);
            }
        };
    }

    public <V> MappingAdapter<Map<String, V>, JsonParser, JsonGenerator> apply(final MappingAdapter<V, JsonParser, JsonGenerator> itemMapper) {
        return new MappingAdapter<Map<String, V>, JsonParser, JsonGenerator>() {
            @Override
            public Map<String, V> toObject(JsonParser source) throws IOException {
                return JsonObjectMapper.this.toObject(source, itemMapper);
            }

            @Override
            public void write(Map<String, V> object, JsonGenerator to) throws IOException {
                JsonObjectMapper.this.write(object, to, itemMapper);
            }
        };
    }
}
