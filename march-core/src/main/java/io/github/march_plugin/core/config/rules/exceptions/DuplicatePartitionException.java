package io.github.march_plugin.core.config.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when an IN expression contains duplicate partitions.
 */
public class DuplicatePartitionException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param partition the name of the duplicated partition.
     */
    public DuplicatePartitionException(final String partition) {
        super("IN expression must not contain the same partition '%s' twice".formatted(partition));
    }
}