package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package dependency violates a specific rule.
 */
public class PackageDependencyForbiddenException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param source the classification of the dependency source
     * @param target the classification of the dependency target
     * @param rule the description of the rule
     * @param detail description of the violation
     */
    public PackageDependencyForbiddenException(final String dependency, final Classification source, final Classification target, final String rule, final String detail) {
        super("[ArchUnit dependency] '" + dependency + "' is forbidden by rule '" + rule + "'. Source: " + source + ", Target: " + target + ". " + detail);
    }
}