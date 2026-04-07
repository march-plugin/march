package io.github.march_plugin.core.rules.exceptions;

/**
 * Thrown when a module dependency is not allowed by any rule.
 */
public class DependencyNotAllowedException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     */
    public DependencyNotAllowedException(final String dependency) {
        super("dependency '" + dependency + "' is not allowed by any rule'");
    }
}