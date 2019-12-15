package com.gitlab.faerytea.mapper.processor

import javax.lang.model.type.TypeMirror

class TypeNotFoundException(val key: TypeMirror): Exception("$key is not found")