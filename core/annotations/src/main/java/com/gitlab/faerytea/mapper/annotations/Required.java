package com.gitlab.faerytea.mapper.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Required property.
 * <p>
 * For serialization: default value (usually {@code null}) here
 * must be serialized (without this annotation such property
 * should be ignored).
 * <p>
 * For parsing: if there is no property then exception must be generated
 * if and only if there is no {@linkplain Default Default.*} annotations.
 *
 * @see Default
 * @see com.gitlab.faerytea.mapper.validation.Validate
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface Required {
    boolean value() default true;
}
