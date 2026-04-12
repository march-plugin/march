package io.github.march_plugin.core.config.dimensions.exceptions;

/**
 * Thrown when a partition is defined twice within dimension.
 */
public class DuplicationPartitionDefinitionException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension that has same partition defined twice.
     * @param partitionName the partition that is defined twice.
     */
    public DuplicationPartitionDefinitionException(final String dimensionName, final String partitionName) {
        super("Dimension '" + dimensionName + "' has duplicate definition of partition '" + partitionName +"'.");
    }
}