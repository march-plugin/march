package io.github.march_plugin.core.config.dimensions.exceptions;

/**
 * Thrown when trying to compare two partitions of different dimensions.
 */
public class InvalidPartitionComparisonException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param partitionName the partition to compare
     * @param dimensionName the dimension of the partitions to compare with
     */
    public InvalidPartitionComparisonException(final String dimensionName, final String partitionName) {
        super("Partition '%s' cannot be compared to partitions of dimension '%s'".formatted(partitionName, dimensionName));
    }
}