package io.github.march_plugin.core.config.dimensions.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a partition name is blank.
 */
public class BlankPartitionNameException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     */
    public BlankPartitionNameException() {
        super("Partition name must not be blank");
    }
}