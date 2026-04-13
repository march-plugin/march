package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when a modularity contains children of both types package modularity and module modularity.
 */
public class DifferentChildModularityTypeException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public DifferentChildModularityTypeException() {
        super("Module Modularity can only contain children of either type package modularity or package modularity");
    }
}