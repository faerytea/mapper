package com.gitlab.faerytea.mapper.annotations;

import com.gitlab.faerytea.mapper.adapters.DoNothing;
import com.gitlab.faerytea.mapper.adapters.UnknownPropertyHandler;

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
     * Specify handler for unknown properties.
     *
     * @return handler
     * @see UnknownPropertyHandler
     * @see DoNothing
     */
    Class<? extends UnknownPropertyHandler> onUnknown() default DoNothing.class;

    /**
     * Specify named instance of unknown property handler.
     * Use only with {@link #onUnknown()}
     *
     * @return name of named instance of {@link UnknownPropertyHandler}
     */
    String onUnknownNamed() default "";
}
