package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when a package name contains dots.
 */
public class IllegalPackageNameException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public IllegalPackageNameException() {
        super("The package name must not contain dots");
    }
}