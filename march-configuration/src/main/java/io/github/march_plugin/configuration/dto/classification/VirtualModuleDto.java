package io.github.march_plugin.configuration.dto.classification;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

public record VirtualModuleDto(
        @JacksonXmlElementWrapper(useWrapping = false)
        List<VirtualModuleDto> virtualModule,
        @JacksonXmlElementWrapper(useWrapping = false)
        List<VirtualModuleRefDto> virtualModuleRef,
        String partition,
        String groupId,
        String virtualArtifactId,
        String virtualGroupId
) {
}
