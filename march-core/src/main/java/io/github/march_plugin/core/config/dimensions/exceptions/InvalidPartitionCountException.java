package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dimension has less than two partitions.
 */
public class InvalidPartitionCountException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension that is not defined in the configuration.
     */
    public InvalidPartitionCountException(final String dimensionName) {
        super("Dimension '" + dimensionName + "' must have at least two partitions.");
    }
}