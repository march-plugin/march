package io.github.march_plugin.core.config.rules.exceptions;

/**
 * Thrown when a comparison is logically redundant (always true or always false).
 */
public class RedundantComparisonException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param expression the string representation of the redundant expression.
     */
    public RedundantComparisonException(final String expression) {
        super("Comparison is redundant (both sides are identical): %s".formatted(expression));
    }
}