package io.github.march_plugin.core.exceptions;

/**
 * Common base type for all exceptions that signal a violation of the configuration.
 */
public class MarchViolationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param message the detail message
     */
    public MarchViolationException(final String message) {
        super(message);
    }

    /**
     * Constructs the exception.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public MarchViolationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
