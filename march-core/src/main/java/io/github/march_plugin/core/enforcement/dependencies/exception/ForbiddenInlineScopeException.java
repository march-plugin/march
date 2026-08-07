package io.github.march_plugin.core.enforcement.dependencies.exception;

import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;

/**
 * Thrown when a dependency defines the module scope inline.
 */
public class ForbiddenInlineScopeException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param module the module defining the dependency.
     * @param scope the defined scope of the dependency.
     * @param dependency the coordinates of the dependency containing the inline scope.
     */
    public ForbiddenInlineScopeException(final ModuleCoordinates module, final String scope, final ModuleCoordinates dependency) {
        super("Module '" + module + "' defines inline scope '" + scope + "' for dependency on module '" + dependency + "'. Use dependency management instead.");
    }
}