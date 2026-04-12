package io.github.march_plugin.core.config.dimensions.exceptions;

/**
 * Thrown when building a group of partitions without partition.
 */
public class EmptyDimensionPartitionGroupException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public EmptyDimensionPartitionGroupException() {
        super("DimensionPartitionGroup must not be empty");
    }
}