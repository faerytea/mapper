package com.gitlab.faerytea.mapper.gen;

import com.gitlab.faerytea.mapper.annotations.Instance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InstanceData {
    /**
     * Fully qualified classname of instance holder
     */
    @NotNull
    public final String holderClassName;
    
    /**
     * Accessor for instance, getter or field name
     */
    @NotNull
    public final String instanceJavaName;

    /**
     * Name of concrete instance
     * @see Instance#value()
     */
    @Nullable
    public final String namedInstanceName;

    /**
     * Is a method?
     */
    public final boolean isMethod;

    public InstanceData(@NotNull CharSequence holderClassName, @NotNull String instanceJavaName, boolean isMethod) {
        this(holderClassName, instanceJavaName, null, isMethod);
    }

    public InstanceData(@NotNull CharSequence holderClassName, @NotNull String instanceJavaName, @Nullable String namedInstanceName, boolean isMethod) {
        this.holderClassName = holderClassName.toString();
        this.instanceJavaName = instanceJavaName;
        this.namedInstanceName = namedInstanceName;
        this.isMethod = isMethod;
    }

    /**
     * Builds a correct java expression which evaluates to
     * instance of adapter {@code className}.
     *
     * @return accessor for adapter
     */
    @NotNull
    public String javaAccessor() {
        return holderClassName + "." + instanceJavaName + (isMethod ? "()" : "");
    }
}
