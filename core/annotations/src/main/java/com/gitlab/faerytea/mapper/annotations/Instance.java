package com.gitlab.faerytea.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks way to get default instance of adapter or converter.
 * Should be used only on {@code public static final} fields or
 * {@code public static} method.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.METHOD,
        ElementType.FIELD
})
public @interface Instance {
}

