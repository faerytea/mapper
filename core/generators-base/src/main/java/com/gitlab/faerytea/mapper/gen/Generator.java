/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Generators.
 *
 * Mapper Generators is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Generators is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Mapper Generators.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.gen;

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ReferenceType;

/**
 * Interface for generating type-specific mappers.
 * Must have constructor with single {@link ProcessingEnvironment}
 * parameter.
 */
public interface Generator {
    /**
     * Generate {@link MappingAdapter} for {@code targetType};
     * fields can be found by its serialized names in {@code fields}.
     * Generated class must implement (directly or indirectly)
     * interface from {@link com.gitlab.faerytea.mapper.adapters}
     * package. Note that you can get primitives as target type.
     *
     * @param targetType type for adapter
     * @param fields     fields of object
     * @param onUnknown  unknown property handler
     * @param validator  class validator
     * @return fully qualified class name and its capabilities
     * @throws GeneratingException when parser / serializer / mapper cannot be generated
     * @throws IOException         when I/O error occurs (see {@link Filer})
     */
    @NotNull
    GeneratedResultInfo generateFor(
            @NotNull TypeElement targetType,
            @NotNull Map<@NotNull String, @NotNull FieldData> fields,
            @NotNull AdapterInfo onUnknown,
            @Nullable ValidatorInfo validator
    ) throws GeneratingException, IOException;

    /**
     * Generate {@link MappingAdapter} for enum {@code targetType};
     * constants can be found by its serialized names in {@code constants}.
     * Generated class must implement (directly or indirectly)
     * interface from {@link com.gitlab.faerytea.mapper.adapters}
     * package.
     *
     * @param targetType       type for adapter
     * @param constants        mapping from possible serialized constants to actual
     *                         enum constants
     * @param onUnknown        unknown constant handler
     * @param stringParser     parser for strings
     * @param stringSerializer serializer for strings
     * @return fully qualified class name and its capabilities
     * @throws GeneratingException when parser / serializer / mapper cannot be generated
     * @throws IOException         when I/O error occurs (see {@link Filer})
     */
    @NotNull
    GeneratedResultInfo generateFor(
            @NotNull TypeElement targetType,
            @NotNull Map<@NotNull String, @NotNull String> constants,
            @NotNull AdapterInfo onUnknown,
            @NotNull AdapterInfo stringParser,
            @NotNull AdapterInfo stringSerializer
    ) throws GeneratingException, IOException;

    /**
     * Generate {@link MappingAdapter} for {@code targetType}
     * which delegates all work to adapters for subtypes.
     * Generated class must implement (directly or indirectly)
     * interface from {@link com.gitlab.faerytea.mapper.adapters}
     * package.
     *
     * @param targetType    type for adapter
     * @param resolver      info about subtypes
     * @param markAsDefault mark generated mapper as default mapper
     * @param validator     class validator
     * @return fully qualified class name and its capabilities
     * @throws GeneratingException when parser / serializer / mapper cannot be generated
     * @throws IOException         when I/O error occurs (see {@link Filer})
     */
    @NotNull
    GeneratedResultInfo generateFor(
            @NotNull TypeElement targetType,
            @NotNull ConcreteTypeResolver resolver,
            boolean markAsDefault,
            @Nullable ValidatorInfo validator
    ) throws GeneratingException, IOException;

    /**
     * Generate name for {@code targetType}'s parser/serializer.
     * This method only will be called if {@link #generateFor(TypeElement, Map, AdapterInfo, ValidatorInfo)}
     * cannot be called due to circular dependencies.
     *
     * @param targetType type for adapter
     * @return same as {@link #generateFor(TypeElement, Map, AdapterInfo, ValidatorInfo)}
     */
    @NotNull
    GeneratedResultInfo nameFor(@NotNull TypeElement targetType);

    /**
     * Returns class name of input.
     *
     * @return class name for type argument {@code Input}
     * of generated {@link Parser}s
     */
    @NotNull
    ReferenceType getInputTypeName();


    /**
     * Returns class name of output.
     *
     * @return class name for type argument {@code Output}
     * of generated {@link Serializer}s
     */
    @NotNull
    ReferenceType getOutputTypeName();

    /**
     * Returns default parsers for this generator.
     * This method should be used when parsers provided via
     * separate library; if they are generated then processor
     * will be capable to find them during processing.
     *
     * @return mapping from types to parsers
     */
    @NotNull
    Map<@NotNull TypeInfo, @NotNull AdapterInfo> getDefaultParsers();

    /**
     * Returns default serializers for this generator.
     * This method should be used when serializers provided via
     * separate library; if they are generated then processor
     * will be capable to find them during processing.
     *
     * @return mapping from types to serializers
     */
    @NotNull
    Map<@NotNull TypeInfo, @NotNull AdapterInfo> getDefaultSerializers();

    /**
     * Returns instances of classes from library
     *
     * @return collection of instances
     */
    @NotNull
    Collection<@NotNull InstanceData> getInstances();

    /**
     * Will be called by processor to notify that {@code cycle} classes
     * depends on each other in list iteration order. These classes
     * may require different instance initialization process.
     * This method will be called before any {@link #generateFor(TypeElement, Map, AdapterInfo, ValidatorInfo)}
     * calls for classes in list.
     *
     * @param cycle dependency cycle
     */
    void notifyCircularDependency(@NotNull List<@NotNull TypeElement> cycle);

    /**
     * Here you can prepare for generating files.
     * This method will be called after successful construction
     * and before first processed file; here you can write helper
     * class with useful static methods like these:
     * <pre>
     * public static String toString(Output out)
     * public static Input fromInputStream(InputStream in)
     * </pre>
     * and generate default mappers.
     */
    default void writePrelude() {
    }

    /**
     * This method will be called in last round.
     * All annotations already processed at this point,
     * so this is a perfect place to generate storage / facade
     * for previously generated classes.
     */
    default void writeEpilogue() {
    }

    @NotNull
    static List<@NotNull String> biggestConstructor(@NotNull Map<@NotNull String, @NotNull FieldData> fields) {
        List<String> largest = Collections.emptyList();
        for (FieldData e : fields.values()) {
            for (Setter s : e.setters) {
                if (s.setterType == Setter.Type.CONSTRUCTOR
                        && s.propertyNames.size() > largest.size())
                    largest = s.propertyNames;
            }
        }
        return largest;
    }
}
