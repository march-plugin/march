package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a module is already classified.
 */
public class DuplicateModuleException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param coordinates the maven coordinates of the module already classified.
     */
    public DuplicateModuleException(final String coordinates) {
        super("Module '%s' is already configured".formatted(coordinates));
    }
}