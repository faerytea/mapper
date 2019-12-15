package com.gitlab.faerytea.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark class as mappable, so code will be generated.
 * Applying to constructor implicitly marks all constructor
 * parameters as {@link Property}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Mappable {
    /**
     * Specify mappers for generator
     *
     * @return fully qualified mapper class
     */
    String[] by() default {};
}
