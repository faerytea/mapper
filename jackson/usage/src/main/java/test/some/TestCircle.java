package test.some;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.Objects;

@Mappable
public class TestCircle implements TestShape {
    @Property
    public final double radius;

    @Mappable
    public TestCircle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return radius * radius * Math.PI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCircle that = (TestCircle) o;
        return Double.compare(that.radius, radius) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(radius);
    }

    @Override
    public String toString() {
        return "TestCircle{" +
                "radius=" + radius +
                '}';
    }
}
