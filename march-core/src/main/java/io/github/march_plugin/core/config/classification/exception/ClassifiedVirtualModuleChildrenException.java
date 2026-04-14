package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when adding children of forbidden type to ClassifiedVirtualModule.
 */
public class ClassifiedVirtualModuleChildrenException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public ClassifiedVirtualModuleChildrenException() {
        super("Virtual Modules can only contain virtual modules as children");
    }
}