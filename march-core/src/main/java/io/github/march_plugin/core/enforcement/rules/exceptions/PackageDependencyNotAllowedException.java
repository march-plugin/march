package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package dependency is not allowed by any rule.
 */
public class PackageDependencyNotAllowedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param detail description of the violation
     */
    public PackageDependencyNotAllowedException(final String dependency, final String detail) {
        super("Dependency '" + dependency + "' is not explicitly allowed by any rule. " + detail);
    }
}