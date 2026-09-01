package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a partition is defined twice within group.
 */
public class GroupDuplicationPartitionDefinitionException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param partitionName the partition that is defined twice.
     */
    public GroupDuplicationPartitionDefinitionException(final String partitionName) {
        super("Partition '%s' cannot be defined twice within the group of partitions".formatted(partitionName));
    }
}