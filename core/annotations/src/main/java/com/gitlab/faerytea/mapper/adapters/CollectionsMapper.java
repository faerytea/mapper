package com.gitlab.faerytea.mapper.adapters;

/**
 * Mappers for generic collections.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this mapper.
 * <br/>
 * Note that type parameter {@code C} will be
 * checked by processor for every
 * {@linkplain com.gitlab.faerytea.mapper.annotations.DefaultMapper DefaultMapper}
 * subclass for restrictions noted in methods' documentation
 *
 * @param <C>     raw type of collection
 * @param <Input> input type
 */
public interface CollectionsMapper<C extends Iterable, Input, Output> extends CollectionsParser<C, Input>, CollectionsSerializer<C, Output> {
    /**
     * Way to create mapper. For {@code @DefaultMapper}s
     * processor will check that return type is actually
     * {@code MappingAdapter<C<T>, Input>}.
     *
     * @param itemMapper mapper for items
     * @param <T>        type of items
     * @return mapper for properly typed collection
     */
    <T> MappingAdapter<? extends C, Input, Output> apply(MappingAdapter<T, Input, Output> itemMapper);
}
