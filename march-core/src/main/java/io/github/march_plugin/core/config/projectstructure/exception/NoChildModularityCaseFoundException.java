package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when no child modularity is defined for a specific partition.
 */
public class NoChildModularityCaseFoundException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param partition the partition to find a child modularity for
     */
    public NoChildModularityCaseFoundException(final String partition) {
        super("No child modularity with case for partition '%s' found".formatted(partition));
    }
}