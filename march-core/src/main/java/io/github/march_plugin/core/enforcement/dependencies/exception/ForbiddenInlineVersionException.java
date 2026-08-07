package io.github.march_plugin.core.enforcement.dependencies.exception;

import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;

/**
 * Thrown when a dependency defines the module version inline.
 */
public class ForbiddenInlineVersionException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param module the module defining the dependency.
     * @param version the defined version of the dependency.
     * @param dependency the coordinates of the dependency containing the inline version.
     */
    public ForbiddenInlineVersionException(final ModuleCoordinates module, final String version, final ModuleCoordinates dependency) {
        super("Module '" + module + "' defines inline version '" + version + "' for dependency on module '" + dependency + "'. Use dependency management instead.");
    }
}