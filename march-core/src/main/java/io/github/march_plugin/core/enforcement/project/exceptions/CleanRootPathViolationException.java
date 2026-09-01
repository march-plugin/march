package io.github.march_plugin.core.enforcement.project.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a module does not have a clean root package path.
 */
public class CleanRootPathViolationException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param module the module with root path violation
     * @param rootPackage the root package specified in march config
     * @param violatingFile the invalid file found
     */
    public CleanRootPathViolationException(final String module, final String rootPackage, final String violatingFile) {
        super("The module '%s' must have a clean root package '%s', but found invalid file '%s'".formatted(module, rootPackage, violatingFile));
    }
}