package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a module is already classified.
 */
public class DuplicateModuleException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param coordinates the maven coordinates of the module already classified.
     */
    public DuplicateModuleException(final String coordinates) {
        super("Module '%s' is already configured".formatted(coordinates));
    }
}