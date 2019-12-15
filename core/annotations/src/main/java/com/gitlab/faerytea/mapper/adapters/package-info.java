/**
 * Package with adapter interfaces.
 * <p>
 * There is no interface for generic adapters because of
 * limitations of java type system. Anyway users will
 * use concrete classes, and you should write or generate type-specific
 * adapters with {@linkplain com.gitlab.faerytea.mapper.adapters.ArrayMapper
   ArrayMapper}-like interface:
 * <ul>
 *     <li>{@code MyType<A, B> toObject(Input input, Mapper<A> ma, Mapper<B> mb)}
 *     <br />, which perform actual parsing</li>
 *     <li>{@code void write(MyType<A, B> obj, Output output, Mapper<A> ma, Mapper<B> mb)}
 *     <br />, which perform actual serializing</li>
 *     <li>{@code Mapper<MyType<A, B>> apply(Mapper<A> ma, Mapper<B> mb)}
 *     <br />, which returns simple {@link com.gitlab.faerytea.mapper.adapters.MappingAdapter}</li>
 * </ul>
 * This also applies for {@linkplain com.gitlab.faerytea.mapper.adapters.Parser Parser}s
 * and {@linkplain com.gitlab.faerytea.mapper.adapters.Serializer Serializer}s
 */
package com.gitlab.faerytea.mapper.adapters;