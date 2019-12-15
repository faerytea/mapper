package com.gitlab.faerytea.mapper.adapters;

public interface Parser<T, Input> {
    T toObject(Input source);
}
