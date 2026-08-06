package io.github.march_plugin.configuration.dto.classification;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

public record ModuleDto(
        @JacksonXmlElementWrapper(useWrapping = false)
        List<ModuleDto> module,
        PackageTemplateRefDto packageTemplate,
        @JacksonXmlElementWrapper(useWrapping = false)
        List<VirtualModuleDto> virtualModule,
        String groupId,
        String artifactId,
        String partition,
        String rootPackage
) {
}
