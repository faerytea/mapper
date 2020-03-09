package test.some;

//@MappableViaSubclasses(@SubtypeResolver(
//        variant = SubtypeResolver.Variant.WRAPPER_KEY,
//        subtypes = {
//                @Subtype(value = Test8.class, name = "t8"),
//                @Subtype(value = Test7.class, name = "t7")
//        }
//))
public interface Test6<T> {
    T getValue();
}
