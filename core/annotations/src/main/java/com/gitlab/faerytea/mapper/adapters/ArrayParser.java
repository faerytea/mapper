package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

/**
 * Parsers for Java's arrays.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this parser
 *
 * @param <Input> input type
 */
public interface ArrayParser<Input> {
    <ComponentTp> ComponentTp[] toObject(Input source, Parser<ComponentTp, Input> componentTpParser) throws IOException;
    <ComponentTp> Parser<ComponentTp[], Input> apply(Parser<ComponentTp, Input> componentParser);
}
