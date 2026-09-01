package io.github.march_plugin.core.config.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a comparison lacks a relative variable (source or target).
 */
public class ConstantComparisonException extends MarchViolationException {

    /**
     * Constructs the exception.
     */
    public ConstantComparisonException() {
        super("Comparison must involve at least one relative expression (source or target) to be evaluatable.");
    }
}