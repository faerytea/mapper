package com.gitlab.faerytea.mapper.converters;

/**
 * Converter interface which helps convert objects
 * between understandable and useful forms
 *
 * @param <From> understandable form (i.e. mappable)
 * @param <To>   useful form (i.e. type in java)
 */
public interface Converter<From, To> extends MarkerConverter {
    To decode(From value);
    From encode(To value);
}
