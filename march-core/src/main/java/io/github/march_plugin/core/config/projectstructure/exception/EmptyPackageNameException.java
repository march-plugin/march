package io.github.march_plugin.core.config.projectstructure.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package name is empty.
 */
public class EmptyPackageNameException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public EmptyPackageNameException() {
        super("The package name must not be empty");
    }
}