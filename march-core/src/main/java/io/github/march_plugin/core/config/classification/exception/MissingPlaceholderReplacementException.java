package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a placeholder could not be replaced.
 */
public class MissingPlaceholderReplacementException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param placeHolder the placeHolder that could not be replaced.
     */
    public MissingPlaceholderReplacementException(final String placeHolder) {
        super("The placeholder '%s' could not be replaced".formatted(placeHolder));
    }
}