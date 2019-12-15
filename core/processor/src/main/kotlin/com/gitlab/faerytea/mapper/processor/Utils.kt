package com.gitlab.faerytea.mapper.processor

import com.gitlab.faerytea.mapper.annotations.Property
import java.io.PrintWriter
import java.io.StringWriter
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import kotlin.reflect.KClass


fun KClass<*>.safeCanonicalName(): CharSequence = try {
    this.java.canonicalName
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}

fun Property.safeUsing(): CharSequence = try {
    this.using.safeCanonicalName()
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}.run {
    if (startsWith("<any?>.", true)) substring(7) else this
}

fun Property.safeUsingSer(): CharSequence = try {
    this.serializeUsing.safeCanonicalName()
} catch (e: MirroredTypeException) {
    ((e.typeMirror as DeclaredType).asElement() as TypeElement).qualifiedName
}.run {
    if (startsWith("<any?>.", true)) substring(7) else this
}

fun Property.safeUsingPar(): CharSequence = try {
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

fun Throwable.stringTrace(): StringBuffer? = StringWriter().apply { this@stringTrace.printStackTrace(PrintWriter(this, true)) }.buffer