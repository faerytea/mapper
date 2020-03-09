package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;
import com.gitlab.faerytea.mapper.converters.Convert;

import java.util.Objects;

import converter.Boxer;

@Mappable
public  class Test8 implements Test6<Integer> {
    private final int value;

    @Mappable
    public Test8(int value) {
        this.value = value;
    }

    @Property
    @Convert(Boxer.class)
    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Test8{" +
                "value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test8 test8 = (Test8) o;
        return value == test8.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
