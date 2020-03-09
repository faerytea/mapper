package converter;

import com.gitlab.faerytea.mapper.converters.Converter;

import java.util.ArrayList;
import java.util.List;

public class ListCaster implements Converter<ArrayList<?>, List<?>> {
    @Override
    public List<?> decode(ArrayList<?> value) {
        return value;
    }

    @Override
    public ArrayList<?> encode(List<?> value) {
        return value instanceof ArrayList ? (ArrayList<?>) value : new ArrayList<>(value);
    }
}
