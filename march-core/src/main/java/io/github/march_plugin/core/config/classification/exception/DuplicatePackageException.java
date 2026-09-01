package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package is already classified.
 */
public class DuplicatePackageException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param coordinates the maven coordinates of the package already classified.
     */
    public DuplicatePackageException(final String coordinates) {
        super("Package '%s' is already configured".formatted(coordinates));
    }
}