package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dimension name is blank.
 */
public class BlankDimensionNameException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     */
    public BlankDimensionNameException() {
        super("Dimension name must not be blank");
    }
}