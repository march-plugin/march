package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a module dependency is not allowed by any rule.
 */
public class DependencyNotAllowedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     */
    public DependencyNotAllowedException(final String dependency) {
        super("dependency '" + dependency + "' is not allowed by any rule'");
    }
}