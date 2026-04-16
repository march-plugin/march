package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a module is not classified.
 */
public class ModuleNotClassifiedException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param moduleCoordinates the coordinates of the module that is not classified.
     */
    public ModuleNotClassifiedException(final String moduleCoordinates) {
        super("Module '%s' is not classified".formatted(moduleCoordinates));
    }
}