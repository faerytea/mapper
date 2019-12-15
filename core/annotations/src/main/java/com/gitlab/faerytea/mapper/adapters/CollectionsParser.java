package com.gitlab.faerytea.mapper.adapters;

/**
 * Parsers for generic collections.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this parser.
 * <br/>
 * Note that type parameter {@code C} will be
 * checked by processor for every
 * {@linkplain com.gitlab.faerytea.mapper.annotations.DefaultParser DefaultParser}
 * subclass for restrictions noted in methods' documentation
 *
 * @param <C>     raw type of collection
 * @param <Input> input type
 */
public interface CollectionsParser<C extends Iterable, Input> {
    /**
     * Main parser method. For {@code @DefaultParser}s
     * processor will check that return type is actually
     * {@code C<T>}.
     *
     * @param source     serialized data
     * @param itemParser parser for items
     * @param <T>        type of items
     * @return properly typed collection
     */
    <T> C toObject(Input source, Parser<T, Input> itemParser);

    /**
     * Way to create parser. For {@code @DefaultParser}s
     * processor will check that return type is actually
     * {@code Parser<C<T>, Input>}.
     *
     * @param itemParser parser for items
     * @param <T>        type of items
     * @return parser for properly typed collection
     */
    <T> Parser<? extends C, Input> apply(Parser<T, Input> itemParser);
}
