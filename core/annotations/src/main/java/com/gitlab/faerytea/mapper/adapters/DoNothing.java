package com.gitlab.faerytea.mapper.adapters;

public class DoNothing implements UnknownPropertyHandler<Object> {
    @Override
    public void handle(String name, Object currentInput) {
        // do nothing
    }
}
