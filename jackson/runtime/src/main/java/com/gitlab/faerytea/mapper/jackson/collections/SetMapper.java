package com.gitlab.faerytea.mapper.jackson.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.gitlab.faerytea.mapper.adapters.CollectionsMapper;
import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;
import com.gitlab.faerytea.mapper.annotations.Instance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DefaultMapper
public class SetMapper implements CollectionsMapper<Set, JsonParser, JsonGenerator> {
    @Instance
    public static final SetMapper INSTANCE = new SetMapper();

    @Override
    public <T> MappingAdapter<Set<T>, JsonParser, JsonGenerator> apply(final MappingAdapter<T, JsonParser, JsonGenerator> itemMapper) {
        return new MappingAdapter<Set<T>, JsonParser, JsonGenerator>() {
            @Override
            public Set<T> toObject(JsonParser source) throws IOException {
                return SetMapper.this.toObject(source, itemMapper);
            }

            @Override
            public void write(Set<T> object, JsonGenerator to) throws IOException {
                SetMapper.this.write(object, to, itemMapper);
            }
        };
    }

    @Override
    public <T> Set<T> toObject(JsonParser source, Parser<T, JsonParser> itemParser) throws IOException {
        final HashSet<T> res = new HashSet<T>();
        JsonToken open = source.currentToken();
        switch (open) {
            case VALUE_NULL:
                source.nextToken();
                return null;
            case START_ARRAY:
                break;
            default:
                throw new JsonParseException(source, "expected array, got " + open);
        }
        source.nextToken();
        while (source.currentToken() != JsonToken.END_ARRAY) {
            res.add(itemParser.toObject(source));
        }
        source.nextToken();
        return res;
    }

    @Override
    public <T> Parser<Set<T>, JsonParser> apply(final Parser<T, JsonParser> itemParser) {
        return new Parser<Set<T>, JsonParser>() {
            @Override
            public Set<T> toObject(JsonParser source) throws IOException {
                return SetMapper.this.toObject(source, itemParser);
            }
        };
    }

    @Override
    public <T> void write(Set object, JsonGenerator to, Serializer<T, JsonGenerator> itemSerializer) throws IOException {
        if (object == null) {
            to.writeNull();
            return;
        }
        to.writeStartArray(object.size());
        @SuppressWarnings("unchecked")
        Set<T> actual = object;
        for (T i : actual) {
            itemSerializer.write(i, to);
        }
        to.writeEndArray();
    }

    @Override
    public <T> Serializer<Set<T>, JsonGenerator> apply(final Serializer<T, JsonGenerator> itemSerializer) {
        return new Serializer<Set<T>, JsonGenerator>() {
            @Override
            public void write(Set<T> object, JsonGenerator to) throws IOException {
                SetMapper.this.write(object, to, itemSerializer);
            }
        };
    }
}
