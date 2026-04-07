package io.github.march_plugin.core.rules.exceptions;

/**
 * Thrown when a module dependency violates a specific rule.
 */
public class DependencyForbiddenException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param rule the description of the rule
     */
    public DependencyForbiddenException(final String dependency, final String rule) {
        super("dependency '" + dependency + "' is forbidden by rule'" + rule + "'.");
    }
}