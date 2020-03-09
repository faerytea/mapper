package test.some;

import com.gitlab.faerytea.mapper.annotations.MappableViaSubclasses;
import com.gitlab.faerytea.mapper.polymorph.Subtype;
import com.gitlab.faerytea.mapper.polymorph.SubtypeResolver;

@MappableViaSubclasses(value = @SubtypeResolver(
        variant = SubtypeResolver.Variant.WRAPPER_KEY,
        subtypes = {
                @Subtype(value = TestCircle.class, name = "circle"),
                @Subtype(value = TestRectangle.class, name = "rect")
        }
), markAsDefault = false)
public interface TestShape {
    double area();
}
