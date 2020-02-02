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

import com.gitlab.faerytea.mapper.annotations.Default
import java.lang.IllegalArgumentException
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleElementVisitor8

object DefaultsVisitor : SimpleElementVisitor8<String, Unit>() {
    override fun visitExecutable(e: ExecutableElement, p: Unit?): String = when (e.parameters.size) {
        0 -> extractDefault(e, e.returnType) // getter
        1 -> extractDefault(e, e.parameters[0].asType()) // simple setter
        else -> throw IllegalArgumentException("$e does not looks like getter or simple setter")
    }

    override fun visitVariable(e: VariableElement, p: Unit?): String = extractDefault(e, e.asType())

    override fun defaultAction(e: Element?, p: Unit?): String = "null"

    private fun extractDefault(e: Element, tp: TypeMirror): String {
        e.getAnnotation(Default::class.java)?.let { return it.value }
        e.getAnnotation(Default.Int::class.java)?.let { return "${it.value}" }
        e.getAnnotation(Default.Long::class.java)?.let { return "${it.value}L" }
        e.getAnnotation(Default.Double::class.java)?.let { return "${it.value}" }
        e.getAnnotation(Default.Bool::class.java)?.let { return "${it.value}" }
        e.getAnnotation(Default.String::class.java)?.let { return "\"${it.value}\"" }
        return when (tp.kind!!) {
            TypeKind.BOOLEAN -> "false"
            TypeKind.BYTE, TypeKind.SHORT, TypeKind.INT, TypeKind.LONG -> "0"
            TypeKind.CHAR -> "'\\0'"
            TypeKind.FLOAT -> "0.0f"
            TypeKind.DOUBLE -> "0.0"
            TypeKind.NULL, TypeKind.ARRAY, TypeKind.DECLARED, TypeKind.ERROR, TypeKind.UNION, TypeKind.INTERSECTION, TypeKind.TYPEVAR -> "null"
            else -> throw IllegalArgumentException("type $tp with kind ${tp.kind} was not expected with element $e")
        }
    }
}