package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when adding children of forbidden type to ClassifiedConcreteModule.
 */
public class ClassifiedConcreteModuleChildrenException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public ClassifiedConcreteModuleChildrenException() {
        super("Concrete modules can only contain either packages or modules");
    }
}