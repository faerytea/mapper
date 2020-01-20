package com.gitlab.faerytea.mapper.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface Validate {
    /**
     * Provide code which will be used for validation.
     * You can use following placeholders:
     * <ul>
     *     <li>{@code $t} for field type on jvm</li>
     *     <li>{@code $e} for enclosing type</li>
     *     <li>{@code $s} for serialized field name</li>
     *     <li>{@code $j} for JVM field name or setter name</li>
     *     <li>{@code $v} for value</li>
     * </ul>
     * Example:
     * {@code "if ($v == null) throw new IllegalStateException(\"$s in $e is null\");"}
     *
     * @return java code
     */
    String value() default "";
    Class<? extends Validator> validator() default Validator.class;
}
