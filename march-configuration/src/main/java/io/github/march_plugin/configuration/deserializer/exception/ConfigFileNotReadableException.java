package io.github.march_plugin.configuration.deserializer.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

import java.io.File;

/**
 * Thrown when the configured March configuration file cannot be read from disk.
 */
public class ConfigFileNotReadableException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param configFile the configuration file that could not be read.
     * @param cause the underlying cause.
     */
    public ConfigFileNotReadableException(final File configFile, final Throwable cause) {
        super("Could not read march configuration file '%s'.".formatted(configFile), cause);
    }
}
