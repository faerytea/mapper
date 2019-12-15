package com.gitlab.faerytea.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides java expression which returns default value.
 * Code, provided in {@code value}, will be invoked if
 * property is not found in serialized form.
 * <p>
 * Do not put complicated logic here, use simple values
 * like "-1" or "null". If necessary, delegate via static methods
 * and put here something like
 * {@code "com.example.MyDefaults.INT_DEFAULT"} (important:
 * not statement, only expression!).
 * <p>
 * Also do not put here impure code or turn off
 * {@link #checkWhileSerialization()} flag.
 *
 * @see Default.Int
 * @see Default.Long
 * @see Default.Double
 * @see Default.Bool
 * @see Default.String
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface Default {
    /**
     * A valid Java expression, which generates default value.
     * No validation before compilation generated classes will be performed!
     *
     * @return expression that generates default value
     */
    java.lang.String value();

    /**
     * If {@code true}, this property will be serialized
     * only if it is not equal (via {@linkplain Object#equals(Object) equals}
     * method or primitive equality) to the value returned by
     * expression from {@link #value()}.
     *
     * @return {@code true} if value should be checked while serialization
     * @see Required
     */
    boolean checkWhileSerialization() default true;

    /**
     * Defaults to provided {@code int}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @interface Int {
        /**
         * @return default value of missing property
         */
        int value();
    }

    /**
     * Defaults to provided {@code long}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @interface Long {
        /**
         * @return default value of missing property
         */
        long value();
    }

    /**
     * Defaults to provided {@code double}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @interface Double {
        /**
         * @return default value of missing property
         */
        double value();
    }

    /**
     * Defaults to provided {@code boolean}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @interface Bool {
        /**
         * @return default value of missing property
         */
        boolean value();
    }

    /**
     * Defaults to provided {@code String}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @interface String {
        /**
         * @return default value of missing property
         */
        java.lang.String value();
    }
}
