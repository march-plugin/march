package io.github.march_plugin.configuration.dto.modularity;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record ModuleModularityDto(
        String dimension,

        @JacksonXmlProperty(isAttribute = true, localName = "case")
        String pCase,
        String allow,
        String groupId,
        String artifactId,
        String rootPackage,

        @JacksonXmlElementWrapper(useWrapping = false)
        List<ModuleModularityDto> modularity,

        @JacksonXmlElementWrapper(useWrapping = false)
        List<PackageModularityDto> packageModularity
) implements ModularityDto {

    @Override
    public String getDimension() {
        return dimension;
    }

    @Override
    public String getCase() {
        return pCase;
    }

    @Override
    public String getAllow() {
        return allow;
    }

    @Override
    public List<PackageModularityDto> getPackageModularity() {
        return packageModularity;
    }
}
