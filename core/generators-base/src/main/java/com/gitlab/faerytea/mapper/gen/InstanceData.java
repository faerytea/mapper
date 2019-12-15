package com.gitlab.faerytea.mapper.gen;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
    public final String instanceName;

    /**
     * Is a method?
     */
    public final boolean isMethod;

    public InstanceData(@NotNull CharSequence holderClassName, @NotNull String instanceName, boolean isMethod) {
        this.holderClassName = holderClassName.toString();
        this.instanceName = instanceName;
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
        return holderClassName + "." + instanceName + (isMethod ? "()" : "");
    }

    @NotNull
    @Override
    public String toString() {
        return "Instance{" +
                "holderClassName=" + holderClassName +
                ", instanceName=" + instanceName +
                ", isMethod=" + isMethod +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceData instance = (InstanceData) o;
        return isMethod == instance.isMethod &&
                holderClassName.equals(instance.holderClassName) &&
                instanceName.equals(instance.instanceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holderClassName, instanceName, isMethod);
    }
}
