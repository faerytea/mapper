package com.gitlab.faerytea.mapper.converters;

import com.gitlab.faerytea.mapper.converters.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
public @interface Convert {
    Class<? extends MarkerConverter> value();
    boolean reversed() default false;
}
