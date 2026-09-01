package io.github.march_plugin.core.config.projectstructure.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the project structure defines two children with the same case partition.
 */
public class DuplicateCasePartitionException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param partitionName the partition that is defined twice.
     */
    public DuplicateCasePartitionException(final String partitionName) {
        super("Duplicate partition in project structure configuration: Partition '" + partitionName + "' must not be defined twice");
    }
}