package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.Objects;

@Mappable
public class Test4 {
    Test4() {}

    @Property
    public Test3 backLink;

    @Mappable
    public Test4(Test3 backLink) {
        this.backLink = backLink;
    }

    @Override
    public String toString() {
        return "Test4{" +
                "backLink=" + backLink +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test4 test4 = (Test4) o;
        return Objects.equals(backLink, test4.backLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backLink);
    }
}
