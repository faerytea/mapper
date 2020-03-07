/*
 * Copyright 2020 Valery Maevsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitlab.faerytea.mapper.polymorph;

import com.gitlab.faerytea.mapper.adapters.ThrowingAdapter;
import com.gitlab.faerytea.mapper.annotations.SpecificMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a way to resolve concrete subtype of property's type.
 * You can use this annotation instead of creating specific
 * {@linkplain com.gitlab.faerytea.mapper.adapters.MappingAdapter Mapper}
 * for class/interface; resolver will be generated as
 * delegating {@code MappingAdapter}.
 *
 * @see Variant
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface SubtypeResolver {
    /**
     * Template for resolver.
     *
     * @return variant
     */
    Variant variant();

    /**
     * Possible concrete classes.
     *
     * @return subtypes
     */
    Subtype[] subtypes();

    /**
     * Adapter for not recognized types.
     *
     * @return adapter for everything except {@link #subtypes()}
     */
    SpecificMapper onUnknown() default @SpecificMapper(using = ThrowingAdapter.class);

    /**
     * Property which denotes class.
     *
     * @return property name.
     */
    String classKey() default "class";

    /**
     * Property which denotes value.
     *
     * @return property name.
     */
    String valueKey() default "value";

    /**
     * Variants of polymorphism. Every value has example of
     * mapping for certain hierarchy. For simplicity only JSON
     * (as simple structured serialization) will be provided.
     * It maps to following java classes (
     * {@linkplain com.gitlab.faerytea.mapper.annotations.Property &#64;Property},
     * {@linkplain com.gitlab.faerytea.mapper.annotations.Mappable &#64;Mappable}
     * and other annotations are omitted for simplicity):
     * <pre>
     *     interface Shape {
     *         double area();
     *     }
     *
     *     public class Circle implements Shape {
     *         public final int radius;
     *         public Circle(int r) { radius = r }
     *
     *         &#64;Override
     *         public double area() { return Math.PI * radius * radius; }
     *     }
     *
     *     public class Square implements Shape {
     *         public final int side;
     *         public Square(int side) { this.side = side }
     *
     *         &#64;Override
     *         public double area() { return side * side; }
     *     }
     *
     *     public class Rectangle implements Shape {
     *         public final int w;
     *         public final int h;
     *         public Rectangle(int width, int height) {
     *             w = width;
     *             h = height;
     *         }
     *
     *         &#64;Override
     *         public double area() { return width * height; }
     *     }
     * </pre>
     * In examples these classes will be mapped with {@code List<Shape>}:
     * {@code asList(new Circle(7), new Square(12), new Rectangle(5,10),
     * new Circle(2))}
     */
    enum Variant {
        /**
         * Generated resolver will map wrapper object with
         * single property which name defines type and value
         * defines actual value. This variant does not require
         * parsing context.
         * <p>
         * Example:
         * <pre>
         *  [
         *      {
         *          "circle": {
         *              "radius": 7
         *          }
         *      },
         *      {
         *          "square": {
         *              "side": 12
         *          }
         *      },
         *      {
         *          "rectangle": {
         *              "width": 5,
         *              "height": 10
         *          }
         *      },
         *      {
         *          "circle": {
         *              "radius": 2
         *          }
         *      }
         *  ]
         * </pre>
         */
        WRAPPER_KEY,

        /**
         * Generated resolver will map wrapper object with
         * two properties: {@link SubtypeResolver#classKey()}
         * which value denotes class and
         * {@link SubtypeResolver#valueKey()} which value
         * denotes actual value. This variant require
         * parsing context.
         * <p>
         * Example:
         * <pre>
         *  [
         *      {
         *          "class": "circle",
         *          "value": {
         *              "radius": 7
         *          }
         *      },
         *      {
         *          "class": "square",
         *          "value": {
         *              "side": 12
         *          }
         *      },
         *      {
         *          "class": "rectangle",
         *          "value": {
         *              "width": 5,
         *              "height": 10
         *          }
         *      },
         *      {
         *          "class": "circle"
         *          "value": {
         *              "radius": 2
         *          }
         *      }
         *  ]
         * </pre>
         */
        WRAPPER_PROPERTY,

        /**
         * Generated resolver will map object with
         * additional property {@link SubtypeResolver#classKey()}
         * which value denotes class. This variant require
         * parsing context.
         * <p>
         * Example:
         * <pre>
         *  [
         *      {
         *          "class": "circle",
         *          "radius": 7
         *      },
         *      {
         *          "side": 12,
         *          "class": "square"
         *      },
         *      {
         *          "width": 5,
         *          "height": 10,
         *          "class": "rectangle"
         *      },
         *      {
         *          "radius": 2,
         *          "class": "circle"
         *      }
         *  ]
         * </pre>
         */
        ADDITIONAL_PROPERTY
    }
}
