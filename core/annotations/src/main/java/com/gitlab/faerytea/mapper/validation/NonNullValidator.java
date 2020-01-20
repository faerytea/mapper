package com.gitlab.faerytea.mapper.validation;

/**
 * Check that field is not null.
 * Can not be used on types, only on fields / methods.
 */
public class NonNullValidator implements Validator<Object> {
    @Override
    public void validate(String serializedName, String jvmName, Class<?> enclosing, Class<?> type, Object value) throws IllegalStateException {
        if (value == null)
            throw new IllegalStateException("Field " + serializedName + " is null (in "
                    + enclosing.getCanonicalName() + ", attempt to access via " + jvmName + ")");
    }
}
