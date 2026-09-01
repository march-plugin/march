package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when adding children of forbidden type to ClassifiedConcreteModule.
 */
public class ClassifiedConcreteModuleChildrenException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public ClassifiedConcreteModuleChildrenException() {
        super("Concrete modules can only contain either packages or modules");
    }
}