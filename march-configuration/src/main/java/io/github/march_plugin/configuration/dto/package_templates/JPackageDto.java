package io.github.march_plugin.configuration.dto.package_templates;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

public record JPackageDto(
        String name,
        String partition,
        Boolean optional,
        @JacksonXmlElementWrapper(useWrapping = false)
        List<JPackageDto> jpackage
) {
}
