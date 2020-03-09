package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.Objects;

@Mappable
public class Test7 implements Test6<String> {
    private final String value;

    @Mappable
    public Test7(String value) {
        this.value = value;
    }

    @Property
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Test7{" +
                "value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test7 test7 = (Test7) o;
        return Objects.equals(value, test7.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
