package converter;

import com.gitlab.faerytea.mapper.converters.DoubleConverter;
import com.gitlab.faerytea.mapper.converters.IntConverter;

public class Boxer implements IntConverter<Integer>, DoubleConverter<Double> {
    @Override
    public int toInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    public Integer fromInt(int value) {
        return value;
    }

    @Override
    public Double fromDouble(double value) {
        return value;
    }

    @Override
    public double toDouble(Double value) {
        return value == null ? Double.NaN : value;
    }
}
