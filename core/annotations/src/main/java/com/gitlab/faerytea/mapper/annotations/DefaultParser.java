package com.gitlab.faerytea.mapper.annotations;

import com.gitlab.faerytea.mapper.adapters.Parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark {@link Parser} as default parser.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DefaultParser {
}
