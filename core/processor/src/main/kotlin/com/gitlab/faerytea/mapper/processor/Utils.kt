/*
 * Copyright © 2020 Valery Maevsky
 * mailto:faerytea@gmail.com
 *
 * This file is part of Mapper Processor.
 *
 * Mapper Processor s free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Mapper Processor s distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapper Processor  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gitlab.faerytea.mapper.processor

import com.gitlab.faerytea.mapper.annotations.SpecificMapper
import com.gitlab.faerytea.mapper.gen.InstanceData
import com.gitlab.faerytea.mapper.gen.ValidatorInfo
import com.gitlab.faerytea.mapper.validation.Validate
import com.gitlab.faerytea.mapper.validation.Validator
import java.io.PrintWriter
import java.io.StringWriter
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import kotlin.reflect.KClass

fun KClass<*>.safeCanonicalName(): CharSequence = try {
    this.java.canonicalName
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}

fun SpecificMapper.safeUsing(): CharSequence = try {
    this.using.safeCanonicalName()
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}.run {
    if (startsWith("<any?>.", true)) substring(7) else this
}

fun SpecificMapper.safeUsingSer(): CharSequence = try {
    this.serializeUsing.safeCanonicalName()
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}.run {
    if (startsWith("<any?>.", true)) substring(7) else this
}

fun SpecificMapper.safeUsingPar(): CharSequence = try {
    this.parseUsing.safeCanonicalName()
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}.run {
    if (startsWith("<any?>.", true)) substring(7) else this
}

fun TypeElement.methods(): List<ExecutableElement> {
    var parent = this.superclass
    var current = this
    val result = ElementFilter.methodsIn(current.enclosedElements).toMutableList()
    while (parent.kind != TypeKind.NONE) {
        current = (parent as DeclaredType).asElement() as TypeElement
        parent = current.superclass
        result += ElementFilter.methodsIn(current.enclosedElements)
    }
    return result
}

fun TypeElement.asDeclaredType(): DeclaredType = asType() as DeclaredType

fun Elements.getTypeElement(clazz: KClass<*>): TypeElement = getTypeElement(clazz.safeCanonicalName())

fun Throwable.stringTrace(): StringBuffer? = StringWriter().apply { this@stringTrace.printStackTrace(PrintWriter(this, true)) }.buffer

internal fun validator(e: Element, instances: HashMap<String, InstanceData>): ValidatorInfo? = e.getAnnotation(Validate::class.java)?.run {
    val name = try {
        this.validator.safeCanonicalName()
    } catch (e: MirroredTypeException) {
        ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
    }.toString()
    return if (name == Validator::class.java.canonicalName.toString()) {
        if (this.value.isEmpty()) null else ValidatorInfo.ValidatorString(this.value)
    } else {
        ValidatorInfo.ValidatorClass(name, instances[name])
    }
}