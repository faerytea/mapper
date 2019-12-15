package com.gitlab.faerytea.mapper.adapters;

public interface MappingAdapter<T, Input, Output> extends Serializer<T, Output>, Parser<T, Input> {
}
