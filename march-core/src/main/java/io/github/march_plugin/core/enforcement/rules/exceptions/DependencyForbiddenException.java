package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a module dependency violates a specific rule.
 */
public class DependencyForbiddenException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param source the classification of the dependency source
     * @param target the classification of the dependency target
     * @param rule the description of the rule
     */
    public DependencyForbiddenException(final String dependency, final Classification source, final Classification target, final String rule) {
        super("[Maven dependency] '" + dependency + "' is forbidden by rule '" + rule + "'. Source: " + source + ", Target: " + target);
    }
}