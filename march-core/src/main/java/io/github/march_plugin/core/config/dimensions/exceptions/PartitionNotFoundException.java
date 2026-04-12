package io.github.march_plugin.core.config.dimensions.exceptions;

/**
 * Thrown when a rule references an undefined partition.
 */
public class PartitionNotFoundException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension of the partition.
     * @param partitionName the partition that is not defined in the configuration.
     */
    public PartitionNotFoundException(final String dimensionName, final String partitionName) {
        super("Partition '" + partitionName + "' is not defined for Dimension '" + dimensionName + "' in March Configuration.");
    }
}