package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.Objects;

import unknown.BlowUp;

@Mappable(onUnknown = BlowUp.class, onUnknownNamed = "bang")
public class Test3 {
    Test3() {}

    @Mappable
    public Test3(Test4 test4) {
        this.test4 = test4;
    }

    @Property
    public Test4 test4;

    @Override
    public String toString() {
        return "Test3{" +
                "test4=" + test4 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test3 test3 = (Test3) o;
        return Objects.equals(test4, test3.test4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(test4);
    }
}
