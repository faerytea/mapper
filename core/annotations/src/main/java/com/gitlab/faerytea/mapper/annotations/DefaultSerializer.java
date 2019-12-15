package com.gitlab.faerytea.mapper.annotations;

import com.gitlab.faerytea.mapper.adapters.Serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark {@link Serializer} as default serializer.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DefaultSerializer {
}
