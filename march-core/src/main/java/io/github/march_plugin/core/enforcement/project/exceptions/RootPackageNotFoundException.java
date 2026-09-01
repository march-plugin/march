package io.github.march_plugin.core.enforcement.project.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the specified root package of a module does not exist.
 */
public class RootPackageNotFoundException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param moduleCoordinates the coordinates of the module
     * @param rootPackage the specified root package of the module
     */
    public RootPackageNotFoundException(final String moduleCoordinates, final String rootPackage) {
        super("The specified root package '%s' of module '%s' does not exist".formatted(rootPackage, moduleCoordinates));
    }
}