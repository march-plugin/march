package io.github.march_plugin;

import io.github.march_plugin.core.exceptions.MarchViolationException;

import java.io.File;
import java.util.List;

/**
 * Thrown when a march configuration file does not match march-config.xsd.
 */
public class MarchConfigSchemaViolationException extends MarchViolationException {

    /**
     * Constructs the exception from a list of collected schema violations.
     *
     * @param configFile the configuration file that failed validation.
     * @param violations the collected, human-readable violation messages.
     */
    public MarchConfigSchemaViolationException(final File configFile, final List<String> violations) {
        super("march configuration file '%s' does not match march-config.xsd:%n%s"
                .formatted(configFile, String.join(System.lineSeparator(), violations)));
    }

    /**
     * Constructs the exception from an underlying XML processing failure.
     *
     * @param configFile the configuration file that failed validation.
     * @param cause the underlying cause.
     */
    public MarchConfigSchemaViolationException(final File configFile, final Throwable cause) {
        super("march configuration file '%s' could not be validated against march-config.xsd: %s"
                .formatted(configFile, cause.getMessage()), cause);
    }
}
