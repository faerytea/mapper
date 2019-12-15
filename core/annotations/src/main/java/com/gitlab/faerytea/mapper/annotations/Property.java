package com.gitlab.faerytea.mapper.annotations;

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks properties. Final fields and getter-like methods (i.e. {@code <Type>
 * methodName()} &mdash; no args and non-void return type) creates 'read' properties,
 * setter-like methods (any return type and exactly one argument) and parameters
 * creates 'write' properties, non-final fields creates both. If 'read' property set
 * encloses 'write' property set then serializer will be generated; if 'write' PS
 * encloses 'read' PS then parser will be generated.
 * </p>
 * <p>
 * <b>Note:</b> if used on method parameter one of these must be satisfied:
 * <ul>
 *     <li>All parameters of method are annotated</li>
 *     <li>Annotated parameter is a parameter of constructor, annotated with
 *     {@link Mappable}</li>
 * </ul>
 * </p>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface Property {
    /**
     * Name of this property. Defaults to field name, parameter name or method name without
     * set/get prefix.
     *
     * @return serialized name of this property
     */
    String value() default "";

    /**
     * Specify {@link MappingAdapter} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}. Overrides {@link #serializeUsing()}
     * and {@link #parseUsing()}.
     *
     * @return adapter
     */
    Class<?> using() default MappingAdapter.class;

    /**
     * Specify {@link Serializer} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}.
     *
     * @return adapter
     */
    Class<?> serializeUsing() default Serializer.class;

    /**
     * Specify {@link Parser} to use for mapping. By default uses adapter,
     * annotated by {@link DefaultMapper}.
     *
     * @return adapter
     */
    Class<?> parseUsing() default Parser.class;
}
