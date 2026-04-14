package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when a package name is empty.
 */
public class EmptyPackageNameException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public EmptyPackageNameException() {
        super("The package name must not be empty");
    }
}