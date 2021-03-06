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
package com.gitlab.faerytea.mapper.converters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify converter for specific property accessor.
 * Converters differs from adapters: first converts
 * between input and objects, second between objects.
 * <p>
 * Example: color string to color int:
 * <pre>
 *     class ColorConverter implements IntConverter&lt;String&gt; {
 *         &#64;Override
 *         public int toInt(String color) {
 *             switch (color.toLowerCase()) {
 *                 case "red": return 0xff0000;
 *                 case "green": return 0x00ff00;
 *                 case "blue": return 0x0000ff;
 *                 case "white": return 0xffffff;
 *                 case "black": return 0x000000;
 *                 default: return Integer.parseInt(color.substring(1), 16);
 *             }
 *         }
 *
 *         &#64;Override
 *         public String fromInt(int color) {
 *             switch (color) {
 *                 case 0xff0000: return "red";
 *                 case 0x00ff00: return "green";
 *                 case 0x0000ff: return "blue";
 *                 case 0xffffff: return "white";
 *                 case 0x000000: return "black";
 *                 default: return String.format("#%06x", color);
 *             }
 *         }
 *     }
 * </pre>
 * Usage:
 * <pre>
 *     &#64;Convert(ColorConverter.class)
 *     &#64;Property
 *     int color;
 * </pre>
 *
 * @see Converter
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Convert {
    Class<? extends MarkerConverter> value();
    boolean reversed() default false;
    String named() default "";
}
