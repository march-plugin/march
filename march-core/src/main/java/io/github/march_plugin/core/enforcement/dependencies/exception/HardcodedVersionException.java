package io.github.march_plugin.core.enforcement.dependencies.exception;

import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dependency's version is a literal value instead of a property reference.
 */
public class HardcodedVersionException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param module the module defining the dependency.
     * @param version the hardcoded version of the dependency.
     * @param dependency the coordinates of the dependency with the hardcoded version.
     */
    public HardcodedVersionException(final ModuleCoordinates module, final String version, final ModuleCoordinates dependency) {
        super("Module '" + module + "' declares hardcoded version '" + version + "' for dependency on module '" + dependency + "'. Use a property instead.");
    }
}
