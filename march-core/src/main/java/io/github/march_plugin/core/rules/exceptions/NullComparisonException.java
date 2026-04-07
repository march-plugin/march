package io.github.march_plugin.core.rules.exceptions;

public class NullComparisonException extends RuntimeException {

    /**
     * Constructs the exception.
     */
    public NullComparisonException() {
        super("Comparison sides must not be null.");
    }
}