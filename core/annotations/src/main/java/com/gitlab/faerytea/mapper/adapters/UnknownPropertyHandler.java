package com.gitlab.faerytea.mapper.adapters;

import java.io.IOException;

/**
 * Handler for unknown properties
 */
public interface UnknownPropertyHandler<Input> {
    /**
     * This code will be invoked when generated parser find
     * unknown property
     *
     * @param name         name of property
     * @param currentInput current input, stateful
     * @throws IOException in case of problems from {@code currentInput}
     */
    void handle(String name, Input currentInput) throws IOException;
}
