package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when a modularity does not specify a dimension but has children.
 */
public class EmptyModularityDimensionException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public EmptyModularityDimensionException() {
        super("The dimension of a modularity must be specified if it has children");
    }
}