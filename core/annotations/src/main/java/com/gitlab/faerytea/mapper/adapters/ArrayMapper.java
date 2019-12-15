package com.gitlab.faerytea.mapper.adapters;

/**
 * Mappers for Java's arrays.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this mapper
 *
 * @param <Input>  input type
 * @param <Output> output type
 */
public interface ArrayMapper<Input, Output> extends ArrayParser<Input>, ArraySerializer<Output> {
    <ComponentTp> MappingAdapter<ComponentTp[], Input, Output> apply(MappingAdapter<ComponentTp, Input, Output> componentMapper);
}
