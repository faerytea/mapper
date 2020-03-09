package mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.annotations.DefaultMapper;

import java.io.IOException;

import test.some.Test6;
import test.some.Test7;
import test.some.Test7Adapter;
import test.some.Test8;
import test.some.Test8Adapter;

@DefaultMapper
public class Test6Mapper implements MappingAdapter<Test6, JsonParser, JsonGenerator> {
    @Override
    public Test6 toObject(JsonParser in) throws IOException {
        Test6 res = null;
        if (in.currentToken() == com.fasterxml.jackson.core.JsonToken.VALUE_NULL) {
            in.nextToken();
            return null;
        }
        in.nextToken();
        final String name;
        if (in.currentToken() != com.fasterxml.jackson.core.JsonToken.FIELD_NAME) {
            name = null;
        } else {
            name = in.currentName();
            in.nextToken();
        }
        if (name != null) {
            if ("value".equals(name)) {
                final JsonToken token = in.currentToken();
                if (token == JsonToken.VALUE_STRING) {
                    res = new Test7(in.getText());
                } else if (token == JsonToken.VALUE_NUMBER_INT) {
                    res = new Test8(in.getIntValue());
                }
            } else {
                System.out.println("unknown property: " + name);
            }
        }
        if (in.currentToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) in.nextToken();
        in.nextToken();
        return res;
    }

    @Override
    public void write(Test6 object, JsonGenerator to) throws IOException {
        if (object instanceof Test7) {
            Test7Adapter.Holder.INSTANCE.write((Test7) object, to);
        } else if (object instanceof Test8) {
            Test8Adapter.Holder.INSTANCE.write((Test8) object, to);
        }
    }
}
