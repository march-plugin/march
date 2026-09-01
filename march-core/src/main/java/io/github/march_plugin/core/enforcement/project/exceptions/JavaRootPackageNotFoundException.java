package io.github.march_plugin.core.enforcement.project.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the root package of a module is specified, but java root does not exist.
 */
public class JavaRootPackageNotFoundException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param moduleCoordinates the coordinates of the module
     */
    public JavaRootPackageNotFoundException(final String moduleCoordinates) {
        super("Module '%s' has a root package configured, but 'src/main/java' does not exist".formatted(moduleCoordinates));
    }
}