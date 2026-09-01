package io.github.march_plugin.core.config.projectstructure.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a dimension appears twice in modularity path.
 */
public class DuplicateDimensionInPathException extends MarchViolationException {

    /**
     * Constructs the exception.
     * @param dimensionName the name of the dimension appearing twice in path
     */
    public DuplicateDimensionInPathException(final String dimensionName) {
        super("Dimension '%s' is already present in the ancestor path.".formatted(dimensionName));
    }
}