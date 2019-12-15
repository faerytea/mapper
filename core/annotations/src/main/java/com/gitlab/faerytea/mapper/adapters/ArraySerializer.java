package com.gitlab.faerytea.mapper.adapters;

/**
 * Serializers for Java's arrays.
 * If serialized form have different notation for
 * objects and collections then collections form
 * should be used for mapping in this serializer
 *
 * @param <Output> output type
 */
public interface ArraySerializer<Output> {
    <ComponentTp> void write(ComponentTp[] object, Output to, Serializer<ComponentTp, Output> componentSerializer);
    <ComponentTp> Serializer<ComponentTp[], Output> apply(Serializer<ComponentTp, Output> componentSerializer);
}
