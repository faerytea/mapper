package converter;

import com.gitlab.faerytea.mapper.annotations.Instance;
import com.gitlab.faerytea.mapper.converters.IntConverter;

public class CStyleEnumConverter implements IntConverter<String> {
    private final String[] constants;

    public CStyleEnumConverter(String[] constants) {
        this.constants = constants;
    }

    @Override
    public int toInt(String value) {
        for (int i = 0; i < constants.length; i++) {
            String s = constants[i];
            if (s.equals(value)) return i;
        }
        return -1;
    }

    @Override
    public String fromInt(int value) {
        return value >= 0 && value < constants.length ? constants[value] : null;
    }

    public static final class Holder {
        @Instance("weekdays")
        public static final CStyleEnumConverter DAYS_OF_WEEK = new CStyleEnumConverter(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"});
        @Instance("roshambo")
        public static final CStyleEnumConverter ROSHAMBO = new CStyleEnumConverter(new String[]{"rock", "paper", "scissors"});
    }
}
