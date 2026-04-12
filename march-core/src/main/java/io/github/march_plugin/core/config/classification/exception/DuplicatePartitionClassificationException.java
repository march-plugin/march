package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a module classifies a dimension twice.
 */
public class DuplicatePartitionClassificationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension that is classified twice
     */
    public DuplicatePartitionClassificationException(final String dimensionName) {
        super("Duplicate classification of Dimension '%s'".formatted(dimensionName));
    }
}