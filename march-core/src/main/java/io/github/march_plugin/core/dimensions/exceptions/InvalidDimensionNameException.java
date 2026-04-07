package io.github.march_plugin.core.dimensions.exceptions;

/**
 * Thrown when a dimension has an invalid name.
 */
public class InvalidDimensionNameException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the forbidden name used.
     */
    public InvalidDimensionNameException(final String dimensionName) {
        super("Dimensions must not be named '%s'".formatted(dimensionName));
    }
}