package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when adding children of forbidden type to ClassifiedPackage.
 */
public class ClassifiedPackageChildrenException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public ClassifiedPackageChildrenException() {
        super("Packages can only contain packages as children");
    }
}