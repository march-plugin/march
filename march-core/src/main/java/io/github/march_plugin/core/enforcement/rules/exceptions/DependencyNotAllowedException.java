package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a module dependency is not allowed by any rule.
 */
public class DependencyNotAllowedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param source the classification of the dependency source
     * @param target the classification of the dependency target
     */
    public DependencyNotAllowedException(final String dependency, final Classification source, final Classification target) {
        super("[Maven dependency] '" + dependency + "' is not allowed by any rule. Source: " + source + ", Target: " + target);
    }
}