package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a module dependency violates a specific rule.
 */
public class DependencyForbiddenException extends MarchViolationException {

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