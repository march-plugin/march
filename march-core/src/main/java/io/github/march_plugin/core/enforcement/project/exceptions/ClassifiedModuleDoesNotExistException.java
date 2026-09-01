package io.github.march_plugin.core.enforcement.project.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a classified module does not exist.
 */
public class ClassifiedModuleDoesNotExistException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param moduleCoordinates the coordinates of the module that does not exist.
     */
    public ClassifiedModuleDoesNotExistException(final String moduleCoordinates) {
        super("The classified module '%s' does not exist".formatted(moduleCoordinates));
    }
}