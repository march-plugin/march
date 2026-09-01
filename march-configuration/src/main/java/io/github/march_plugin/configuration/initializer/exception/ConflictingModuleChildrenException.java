package io.github.march_plugin.configuration.initializer.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a classified module declares both child modules (or virtual modules) and a package template.
 */
public class ConflictingModuleChildrenException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param moduleCoordinates the coordinates of the module declaring conflicting children.
     */
    public ConflictingModuleChildrenException(final String moduleCoordinates) {
        super("Module '%s' can only have either child modules or a package template, not both.".formatted(moduleCoordinates));
    }
}
