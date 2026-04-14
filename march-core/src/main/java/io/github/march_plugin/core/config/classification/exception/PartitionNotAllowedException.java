package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a component classifies a partition that is not allowed by parent.
 */
public class PartitionNotAllowedException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param partitionName the partition the component classifies
     * @param allowedPartitions the partitions allowed by parent
     */
    public PartitionNotAllowedException(final String partitionName, final String allowedPartitions) {
        super("Partition '%s' is not contained in allow list of parent %s".formatted(partitionName, allowedPartitions));
    }
}