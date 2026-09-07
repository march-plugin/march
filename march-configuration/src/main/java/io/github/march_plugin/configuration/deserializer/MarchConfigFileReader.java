package io.github.march_plugin.configuration.deserializer;

import io.github.march_plugin.configuration.deserializer.exception.ConfigFileNotReadableException;
import io.github.march_plugin.configuration.dto.MarchConfigDto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads and deserializes the March configuration file from disk.
 */
public class MarchConfigFileReader {

    private final File configFile;

    /**
     * Constructs the reader.
     *
     * @param configFile the march config defined by users
     */
    public MarchConfigFileReader(final File configFile) {
        this.configFile = configFile;
    }

    /**
     * Reads and deserializes the configured March configuration file.
     *
     * @return the deserialized March configuration
     */
    public MarchConfigDto readConfig() {
        try (FileInputStream fis = new FileInputStream(configFile)) {
            return new XmlMarchLoader().deserializeMarchConfigDto(fis);
        } catch (final IOException e) {
            throw new ConfigFileNotReadableException(configFile, e);
        }
    }
}
