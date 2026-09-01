package io.github.march_plugin.core.config.projectstructure.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the dimension of partitions in case does not match the parent partition dimension.
 */
public class UnequalCaseDimensionException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param caseDimension the dimension of the case partitions.
     * @param parentDimension the dimension of parent modularity.
     */
    public UnequalCaseDimensionException(final String caseDimension, final String parentDimension) {
        super("The dimension of case '%s' does not match the dimension of parent '%s'".formatted(caseDimension, parentDimension));
    }
}