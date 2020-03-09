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

@DefaultMapper
public class ArrayListMapper implements CollectionsMapper<ArrayList, JsonParser, JsonGenerator> {
    @Instance
    public static final ArrayListMapper INSTANCE = new ArrayListMapper();

    @Override
    public <T> MappingAdapter<ArrayList<T>, JsonParser, JsonGenerator> apply(final MappingAdapter<T, JsonParser, JsonGenerator> itemMapper) {
        return new MappingAdapter<ArrayList<T>, JsonParser, JsonGenerator>() {
            @Override
            public ArrayList<T> toObject(JsonParser source) throws IOException {
                return ArrayListMapper.this.toObject(source, itemMapper);
            }

            @Override
            public void write(ArrayList<T> object, JsonGenerator to) throws IOException {
                ArrayListMapper.this.write(object, to, itemMapper);
            }
        };
    }

    @Override
    public <T> ArrayList<T> toObject(final JsonParser source, final Parser<T, JsonParser> itemParser) throws IOException {
        final ArrayList<T> res = new ArrayList<T>();
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
    public <T> Parser<ArrayList<T>, JsonParser> apply(final Parser<T, JsonParser> itemParser) {
        return new Parser<ArrayList<T>, JsonParser>() {
            @Override
            public ArrayList<T> toObject(JsonParser source) throws IOException {
                return ArrayListMapper.this.toObject(source, itemParser);
            }
        };
    }

    @Override
    public <T> void write(ArrayList object, JsonGenerator to, Serializer<T, JsonGenerator> itemSerializer) throws IOException {
        if (object == null) {
            to.writeNull();
            return;
        }
        to.writeStartArray(object.size());
        @SuppressWarnings("unchecked")
        ArrayList<T> actual = object;
        for (T i : actual) {
            itemSerializer.write(i, to);
        }
        to.writeEndArray();
    }

    @Override
    public <T> Serializer<ArrayList<T>, JsonGenerator> apply(final Serializer<T, JsonGenerator> itemSerializer) {
        return new Serializer<ArrayList<T>, JsonGenerator>() {
            @Override
            public void write(ArrayList<T> object, JsonGenerator to) throws IOException {
                ArrayListMapper.this.write(object, to, itemSerializer);
            }
        };
    }
}
