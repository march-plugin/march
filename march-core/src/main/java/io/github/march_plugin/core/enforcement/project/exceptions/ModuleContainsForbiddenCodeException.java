package io.github.march_plugin.core.enforcement.project.exceptions;

/**
 * Thrown when a module contains code but has no root package specified.
 */
public class ModuleContainsForbiddenCodeException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param moduleCoordinates the coordinates of the module without specified root package
     */
    public ModuleContainsForbiddenCodeException(final String moduleCoordinates) {
        super("The module '%s' has no root package specified and must not have a src directory.".formatted(moduleCoordinates));
    }
}