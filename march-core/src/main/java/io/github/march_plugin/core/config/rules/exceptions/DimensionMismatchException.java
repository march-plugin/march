package io.github.march_plugin.core.config.rules.exceptions;

/**
 * Thrown when a comparison involves incompatible dimensions.
 */
public class DimensionMismatchException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param expected the dimension name expected.
     * @param actual   the dimension name provided.
     */
    public DimensionMismatchException(final String expected, final String actual) {
        super("Dimension mismatch: Expected '%s' but found '%s'.".formatted(expected, actual));
    }
}