package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when building a group of partitions without partition.
 */
public class EmptyDimensionPartitionGroupException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public EmptyDimensionPartitionGroupException() {
        super("DimensionPartitionGroup must not be empty");
    }
}