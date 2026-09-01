package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dimension is defined twice in the configuration.
 */
public class DuplicationDimensionDefinitionException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension that is defined twice.
     */
    public DuplicationDimensionDefinitionException(final String dimensionName) {
        super("Dimension '%s' is defined twice".formatted(dimensionName));
    }
}