package io.github.march_plugin.core.enforcement.dependencies.exception;

import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dependency in dependency management does not define the version.
 */
public class VersionNotDefinedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param module the module defining the dependency.
     * @param dependency the coordinates of the dependency without defined version.
     */
    public VersionNotDefinedException(final ModuleCoordinates module, final ModuleCoordinates dependency) {
        super("Module '" + module + "' does not define version for dependency on module '" + dependency + "'.");
    }
}