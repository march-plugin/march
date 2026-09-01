package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a rule is not specified in configuration.
 */
public class DimensionNotFoundException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the name of the dimension that is not defined in the configuration.
     */
    public DimensionNotFoundException(final String dimensionName) {
        super("Dimension '%s' is not defined in March Configuration.".formatted(dimensionName));
    }
}