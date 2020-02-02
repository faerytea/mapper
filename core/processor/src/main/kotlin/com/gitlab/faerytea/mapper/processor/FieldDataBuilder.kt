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

import com.gitlab.faerytea.mapper.gen.Getter
import com.gitlab.faerytea.mapper.gen.Setter
import com.gitlab.faerytea.mapper.gen.ValidatorInfo
import javax.lang.model.type.TypeMirror

class FieldDataBuilder(
        val name: String,
        val tp: TypeMirror,
        val required: Boolean,
        val validator: ValidatorInfo? = null,
        val getters: MutableList<Getter> = ArrayList(1),
        val setters: MutableList<Setter> = ArrayList(2)
) {
    override fun toString(): String = "FieldDataBuilder($name, $tp, $getters, $setters)"
}