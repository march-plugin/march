package io.github.march_plugin.core.config.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

public class NullComparisonException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public NullComparisonException() {
        super("Comparison sides must not be null.");
    }
}