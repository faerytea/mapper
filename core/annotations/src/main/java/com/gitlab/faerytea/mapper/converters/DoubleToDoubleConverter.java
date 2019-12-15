package com.gitlab.faerytea.mapper.converters;

public interface DoubleToDoubleConverter extends MarkerConverter {
    double encode(double value);
    double decode(double value);
}
