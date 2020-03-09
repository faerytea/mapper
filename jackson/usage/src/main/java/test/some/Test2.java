package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;
import com.gitlab.faerytea.mapper.validation.Validate;

import java.util.Objects;

import validator.AnswerValidator;

@Mappable
public class Test2 {
    private final int no;
    @Property
    public final String name;
    @Property
    @Validate(validator = AnswerValidator.class)
    public int answer;

    @Mappable
    public Test2(int no, String name, int answer) {
        this.no = no;
        this.name = name;
        this.answer = answer;
    }

    @Property
    public int getNo() {
        return no;
    }

    @Override
    public String toString() {
        return "Test2{" +
                "no=" + no +
                ", name='" + name + '\'' +
                ", answer=" + answer +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test2 test2 = (Test2) o;
        return no == test2.no &&
                answer == test2.answer &&
                Objects.equals(name, test2.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(no, name, answer);
    }
}
