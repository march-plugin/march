package io.github.march_plugin.core.dimensions.exceptions;

/**
 * Thrown when a dimension name is blank.
 */
public class BlankDimensionNameException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     */
    public BlankDimensionNameException() {
        super("Dimension name must not be blank");
    }
}