package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package dependency is not allowed by any rule.
 */
public class PackageDependencyNotAllowedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param source the classification of the dependency source
     * @param target the classification of the dependency target
     * @param detail description of the violation
     */
    public PackageDependencyNotAllowedException(final String dependency, final Classification source, final Classification target, final String detail) {
        super("[ArchUnit dependency] '" + dependency + "' is not explicitly allowed by any rule. Source: " + source + ", Target: " + target + ". " + detail);
    }
}