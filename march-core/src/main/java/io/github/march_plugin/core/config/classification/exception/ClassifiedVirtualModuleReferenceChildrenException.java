package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when adding children of forbidden type to ClassifiedVirtualModuleReference.
 */
public class ClassifiedVirtualModuleReferenceChildrenException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public ClassifiedVirtualModuleReferenceChildrenException() {
        super("Virtual Module References must not contain children");
    }
}