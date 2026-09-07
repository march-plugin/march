package io.github.march_plugin.core.config.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a logical AND/OR combines two identical operands.
 */
public class RedundantLogicalOperationException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param expression the string representation of the redundant operand.
     */
    public RedundantLogicalOperationException(final String expression) {
        super("Logical operation is redundant (both sides are identical): %s".formatted(expression));
    }
}
