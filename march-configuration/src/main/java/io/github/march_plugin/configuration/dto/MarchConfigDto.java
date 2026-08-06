package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.classification.ModulesDto;
import io.github.march_plugin.configuration.dto.dimensions.DimensionDto;
import io.github.march_plugin.configuration.dto.modularity.ProjectStructureDto;
import io.github.march_plugin.configuration.dto.package_templates.PackageTemplatesDto;
import io.github.march_plugin.configuration.dto.rules.RulesDto;

import java.util.List;

public record MarchConfigDto(
        List<DimensionDto> dimensions,
        ProjectStructureDto projectStructure,
        PackageTemplatesDto packageTemplates,
        ModulesDto modules,
        RulesDto rules
) {}
