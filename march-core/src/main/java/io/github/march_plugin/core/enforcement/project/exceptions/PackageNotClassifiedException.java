package io.github.march_plugin.core.enforcement.project.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when an existing package is not classified.
 */
public class PackageNotClassifiedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param packageName the package that is not classified in march config
     * @param moduleCoordinates the coordinates of the module the package is in
     */
    public PackageNotClassifiedException(final String packageName, final String moduleCoordinates) {
        super("The package '%s' in module '%s' is not classified in march config.".formatted(packageName, moduleCoordinates));
    }
}