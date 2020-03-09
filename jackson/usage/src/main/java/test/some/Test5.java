package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;
import com.gitlab.faerytea.mapper.validation.Validate;

import java.util.Objects;

import validator.AnswerValidator;

@Mappable
@Validate(validator = AnswerValidator.class)
public class Test5 {
    @Property
    public final Test5 selfRef;
    @Property
    public final int value;

    @Mappable
    public Test5(Test5 selfRef, int value) {
        this.selfRef = selfRef;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Test5{" +
                "selfRef=" + selfRef +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test5 test5 = (Test5) o;
        return value == test5.value &&
                Objects.equals(selfRef, test5.selfRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selfRef, value);
    }
}
