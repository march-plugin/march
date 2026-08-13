package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when a modularity does not specify a dimension but has children.
 */
public class EmptyModularityDimensionException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param location a description of the modularity/packageModularity node missing the dimension.
     */
    public EmptyModularityDimensionException(final String location) {
        super(("The following project structure node must declare a dimension, because it has children: %s")
                .formatted(location));
    }
}