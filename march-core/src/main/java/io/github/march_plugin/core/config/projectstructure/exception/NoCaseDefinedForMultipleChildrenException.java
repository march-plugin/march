package io.github.march_plugin.core.config.projectstructure.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when case partitions are not defined when parent has multiple children.
 */
public class NoCaseDefinedForMultipleChildrenException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public NoCaseDefinedForMultipleChildrenException() {
        super("Case must be defined if modularity has multiple children");
    }
}