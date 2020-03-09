package converter;

import com.gitlab.faerytea.mapper.converters.Converter;

public class CharSequenceCaster implements Converter<String, CharSequence> {
    @Override
    public CharSequence decode(String value) {
        return value;
    }

    @Override
    public String encode(CharSequence value) {
        return value == null ? null : value.toString();
    }
}
