package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when adding children of forbidden type to ClassifiedVirtualModuleReference.
 */
public class ClassifiedVirtualModuleReferenceChildrenException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public ClassifiedVirtualModuleReferenceChildrenException() {
        super("Virtual Module References must not contain children");
    }
}