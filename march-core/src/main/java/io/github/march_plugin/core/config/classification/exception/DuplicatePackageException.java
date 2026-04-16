package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a package is already classified.
 */
public class DuplicatePackageException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param coordinates the maven coordinates of the package already classified.
     */
    public DuplicatePackageException(final String coordinates) {
        super("Package '%s' is already configured".formatted(coordinates));
    }
}