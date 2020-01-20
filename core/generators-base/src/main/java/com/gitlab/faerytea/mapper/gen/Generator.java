package com.gitlab.faerytea.mapper.gen;

import com.gitlab.faerytea.mapper.adapters.MappingAdapter;
import com.gitlab.faerytea.mapper.adapters.Parser;
import com.gitlab.faerytea.mapper.adapters.Serializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeMirror;

/**
 * Interface for generating type-specific mappers.
 * Must have constructor with single {@link ProcessingEnvironment}
 * parameter.
 */
public interface Generator {
    /**
     * Generate {@link MappingAdapter} for {@code targetType};
     * fields can be found by its serialized names in {@code field}.
     * Generated class must implement (directly or indirectly)
     * interface from {@link com.gitlab.faerytea.mapper.adapters}
     * package. Note that you can get primitives as target type.
     *
     * @param targetType type for adapter
     * @param fields     fields of object
     * @return fully qualified class name and its capabilities
     * @throws GeneratingException when parser / serializer / mapper cannot be generated
     * @throws IOException         when I/O error occurs (see {@link Filer})
     */
    @NotNull
    GeneratedResultInfo generateFor(
            @NotNull TypeElement targetType,
            @NotNull Map<@NotNull String, @NotNull FieldData> fields,
            @Nullable ValidatorInfo validator
    ) throws GeneratingException, IOException;

    /**
     * Generate name for {@code targetType}'s parser/serializer.
     * This method only will be called if {@link #generateFor(TypeElement, Map, ValidatorInfo)}
     * cannot be called due to circular dependencies.
     *
     * @param targetType type for adapter
     * @return same as {@link #generateFor(TypeElement, Map, ValidatorInfo)}
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
    Map<@NotNull TypeMirror, @NotNull AdapterInfo> getDefaultParsers();

    /**
     * Returns default serializers for this generator.
     * This method should be used when serializers provided via
     * separate library; if they are generated then processor
     * will be capable to find them during processing.
     *
     * @return mapping from types to serializers
     */

    Map<@NotNull TypeMirror, @NotNull AdapterInfo> getDefaultSerializers();

    /**
     * Will be called by processor to notify that {@code cycle} classes
     * depends on each other in list iteration order. These classes
     * may require different instance initialization process.
     * This method will be called before any {@link #generateFor(TypeElement, Map, ValidatorInfo)}
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
