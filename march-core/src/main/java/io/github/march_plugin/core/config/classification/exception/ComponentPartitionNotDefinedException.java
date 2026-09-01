package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a component does not specify a partition.
 */
public class ComponentPartitionNotDefinedException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param coordinates the coordinates of the component
     */
    public ComponentPartitionNotDefinedException(final String coordinates) {
        super("The component '%s' must classify a partition".formatted(coordinates));
    }
}