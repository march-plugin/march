package io.github.march_plugin.core.enforcement.rules.exceptions;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when {@code dependencyConfig} is {@code LEAVES_ONLY} and a module dependency's source or target
 * is not a fully classified leaf module, regardless of what any rule would otherwise allow.
 */
public class NonLeafMavenDependencyException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param source the classification of the dependency source
     * @param target the classification of the dependency target
     */
    public NonLeafMavenDependencyException(final String dependency, final Classification source, final Classification target) {
        super("[Maven dependency] '" + dependency + "' involves a non-leaf module, forbidden by dependencyConfig=LEAVES_ONLY. Source: " + source + ", Target: " + target);
    }
}
