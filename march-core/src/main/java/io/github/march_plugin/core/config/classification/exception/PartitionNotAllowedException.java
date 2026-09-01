package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a component classifies a partition that is not allowed by parent.
 */
public class PartitionNotAllowedException extends MarchViolationException {

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