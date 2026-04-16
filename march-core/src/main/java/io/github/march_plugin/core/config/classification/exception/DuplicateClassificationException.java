package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when the same classification exists twice.
 */
public class DuplicateClassificationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param classification the classification that exists twice
     * @param component1 the classified component with matching classification
     * @param component2 the classified component with matching classification
     */
    public DuplicateClassificationException(final String classification, final String component1, final String component2) {
        super("The same classification '%s' exists for components '%s' and '%s'".formatted(classification, component1, component2));
    }
}