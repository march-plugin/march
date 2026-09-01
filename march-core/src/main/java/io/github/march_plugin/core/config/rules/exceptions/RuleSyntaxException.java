package io.github.march_plugin.core.config.rules.exceptions;

import io.github.march_plugin.core.exceptions.MarchViolationException;
import java.util.List;

/**
 * Thrown when a rule cannot be parsed.
 */
public class RuleSyntaxException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param message the detail message
     **/
    public RuleSyntaxException(final String message) {
        super("Rule Syntax Error: " + message);
    }

    /**
     * Factory method to build a detailed error report.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @param cursor the cursor position of the error
     * @param tokens the tokens at the cursor position
     *
     * @return the constructed exception
     */
    public static RuleSyntaxException of(final String expected, final String actual, final int cursor, final List<String> tokens) {
        final var sb = new StringBuilder();
        sb.append("Expected ")
                .append(expected)
                .append(" but found '")
                .append(actual)
                .append("'")
                .append(" at position: ")
                .append(cursor)
                .append(". ")
                .append("Context: ");
        final var start = Math.max(0, cursor - 3);
        final var end = Math.min(tokens.size(), cursor + 4);

        for (var i = start; i < end; i++) {
            var val = tokens.get(i);

            if (val == null || val.isEmpty()) {
                val = "EOF";
            }
            if (i == cursor) {
                sb.append(">>>").append(val).append("<<< ");
            } else {
                sb.append(val).append(" ");
            }
        }

        return new RuleSyntaxException(sb.toString());
    }
}