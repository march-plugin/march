package io.github.march_plugin.core.config.dimensions.exceptions;

/**
 * Thrown when a dimension has less than two partitions.
 */
public class InvalidPartitionCountException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension that is not defined in the configuration.
     */
    public InvalidPartitionCountException(final String dimensionName) {
        super("Dimension '" + dimensionName + "' must have at least two partitions.");
    }
}