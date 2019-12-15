package com.gitlab.faerytea.mapper.adapters;

/**
 * Serializers for generic collections.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this serializer.
 * <br/>
 * Note that type parameter {@code C} will be
 * checked by processor for every
 * {@linkplain com.gitlab.faerytea.mapper.annotations.DefaultSerializer DefaultSerializer}
 * subclass for restrictions noted in methods' documentation
 *
 * @param <C>      raw type of collection
 * @param <Output> output type
 */
public interface CollectionsSerializer<C extends Iterable, Output> {
    /**
     * Main serializer method. Due to limitations of
     * java type system {@code object}'s type remains raw.
     *
     * @param object         what to serialize
     * @param itemSerializer serializer for items
     * @param to             serialized data
     * @param <T>            type of items
     */
    <T> void write(C object, Output to, Serializer<T, Output> itemSerializer);

    /**
     * Way to create serializer. For {@code @DefaultSerializer}s
     * processor will check that return type is actually
     * {@code Serializer<C<T>, Output>}.
     *
     * @param itemSerializer serializer for items
     * @param <T>            type of items
     * @return serializer for properly typed collection
     */
    <T> Serializer<? extends C, Output> apply(Serializer<T, Output> itemSerializer);
}
