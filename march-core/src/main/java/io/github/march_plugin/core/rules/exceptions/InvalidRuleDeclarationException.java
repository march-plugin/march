package io.github.march_plugin.core.rules.exceptions;

/**
 * Thrown when a rule does not have the correct syntax.
 */
public class InvalidRuleDeclarationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param value the part of the rule that does not match (source|target).{dimension} or {dimension}.{partition}
     */
    public InvalidRuleDeclarationException(final String value) {
        super("'" + value + "' does not match format (source|target).<dimension> or <dimension>.<partition>");
    }
}