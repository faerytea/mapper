package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.Objects;

@Mappable
public class TestRectangle implements TestShape {
    @Property
    public final double width;
    @Property
    public final double height;

    @Mappable
    public TestRectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestRectangle that = (TestRectangle) o;
        return Double.compare(that.width, width) == 0 &&
                Double.compare(that.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "TestRectangle{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
