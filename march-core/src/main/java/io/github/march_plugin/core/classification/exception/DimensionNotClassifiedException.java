package io.github.march_plugin.core.classification.exception;

/**
 * Thrown when an excepted dimension is not classified.
 */
public class DimensionNotClassifiedException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dimensionName the dimension that is not defined in the configuration.
     */
    public DimensionNotClassifiedException(final String dimensionName) {
        super("Dimension '%s' is not classified by the module.".formatted(dimensionName));
    }
}