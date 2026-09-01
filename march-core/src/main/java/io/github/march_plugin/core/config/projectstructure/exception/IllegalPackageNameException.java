package io.github.march_plugin.core.config.projectstructure.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package name contains dots.
 */
public class IllegalPackageNameException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public IllegalPackageNameException() {
        super("The package name must not contain dots");
    }
}