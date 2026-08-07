package io.github.march_plugin.configuration.deserializer;

import io.github.march_plugin.configuration.dto.MarchConfigDto;
import tools.jackson.dataformat.xml.XmlMapper;

import java.io.InputStream;

/**
 * Deserializes XML configuration into config DTOs.
 */
public class XmlMarchLoader {
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * Deserializes XML configuration into MarchConfigDto.
     *
     * @param inputStream representing MarchConfigDto as xml
     * @return the deserialized MarchConfigDto
     */
    public MarchConfigDto deserializeMarchConfigDto(final InputStream inputStream) {
        return xmlMapper.readValue(inputStream, MarchConfigDto.class);
    }
}