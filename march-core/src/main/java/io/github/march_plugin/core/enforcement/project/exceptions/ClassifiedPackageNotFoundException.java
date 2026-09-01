package io.github.march_plugin.core.enforcement.project.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a mandatory classified package does not exist.
 */
public class ClassifiedPackageNotFoundException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param packageCoordinates the coordinates of the classified package
     */
    public ClassifiedPackageNotFoundException(final String packageCoordinates) {
        super("The mandatory classified package '%s' does not exist.".formatted(packageCoordinates));
    }
}