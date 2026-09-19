package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when {@code dependencyConfig} is {@code LEAVES_ONLY} and a package dependency's source or target
 * is not a fully classified leaf package, regardless of what any rule would otherwise allow.
 */
public class NonLeafPackageDependencyException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param source the classification of the dependency source
     * @param target the classification of the dependency target
     * @param detail description of the violation
     */
    public NonLeafPackageDependencyException(final String dependency, final Classification source, final Classification target, final String detail) {
        super("[ArchUnit dependency] '" + dependency + "' involves a non-leaf package, forbidden by dependencyConfig=LEAVES_ONLY. Source: " + source + ", Target: " + target + ". " + detail);
    }
}
