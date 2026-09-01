package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dimension has an invalid name.
 */
public class InvalidDimensionNameException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the forbidden name used.
     */
    public InvalidDimensionNameException(final String dimensionName) {
        super("Dimensions must not be named '%s'".formatted(dimensionName));
    }
}