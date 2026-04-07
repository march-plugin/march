package io.github.march_plugin.core.dimensions.exceptions;

/**
 * Thrown when a partition name is blank.
 */
public class BlankPartitionNameException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     */
    public BlankPartitionNameException() {
        super("Partition name must not be blank");
    }
}