package com.gitlab.faerytea.mapper.processor

import com.gitlab.faerytea.mapper.gen.Getter
import com.gitlab.faerytea.mapper.gen.Setter
import javax.lang.model.type.TypeMirror

class FieldDataBuilder(
        val name: String,
        val tp: TypeMirror,
        val required: Boolean,
        val getters: MutableList<Getter> = ArrayList(1),
        val setters: MutableList<Setter> = ArrayList(2)
) {
    override fun toString(): String = "FieldDataBuilder($name, $tp, $getters, $setters)"
}