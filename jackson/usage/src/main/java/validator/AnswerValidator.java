package validator;

import com.gitlab.faerytea.mapper.annotations.Instance;
import com.gitlab.faerytea.mapper.validation.Validator;
import com.gitlab.faerytea.mapper.validation.ValidatorInt;

import test.some.Test5;

public class AnswerValidator implements ValidatorInt, Validator<Test5> {
    @Override
    public void validate(String serializedName, String jvmName, Class<?> enclosing, int value) throws IllegalStateException {
        if (value != 42)
            System.err.println("Answer always is 42! (in " + enclosing + ">" + jvmName + " which is " + value + ")");
    }

    @Override
    public void validate(String serializedName, String jvmName, Class<?> enclosing, Class<? extends Test5> type, Test5 value) throws IllegalStateException {
        if (value.selfRef == value) {
            throw new IllegalStateException("Test5#" + System.identityHashCode(value) + " is self referenced!");
        }
    }

    @Instance
    public static AnswerValidator getInstance() {
        return new AnswerValidator();
    }
}
