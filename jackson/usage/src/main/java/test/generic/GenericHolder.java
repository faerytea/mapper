package test.generic;

import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.Objects;

@Mappable
public class GenericHolder<A> {
    @Property
    public final A a;

    @Mappable
    public GenericHolder(A a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return "GenericHolder{" +
                "a=" + a +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericHolder<?> that = (GenericHolder<?>) o;
        return Objects.equals(a, that.a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a);
    }
}
