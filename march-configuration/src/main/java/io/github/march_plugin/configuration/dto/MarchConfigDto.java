package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.classification.ModulesDto;
import io.github.march_plugin.configuration.dto.dimensions.DimensionDto;
import io.github.march_plugin.configuration.dto.modularity.ProjectStructureDto;
import io.github.march_plugin.configuration.dto.package_templates.PackageTemplatesDto;
import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;

import java.util.List;

public record MarchConfigDto(
        List<DimensionDto> dimensions,
        ProjectStructureDto projectStructure,
        PackageTemplatesDto packageTemplates,
        ModulesDto modules,
        SettingsDto settings,
        List<RuleDto> rules
) {

    /**
     * The declared rules, or an empty list if {@code <rules>} was omitted entirely.
     */
    public List<RuleDto> rules() {
        return rules == null ? List.of() : rules;
    }

    /**
     * The rule engine settings, or {@code null} if {@code <settings>} or {@code <ruleEngine>} was omitted.
     */
    public RuleConfigurationDto ruleEngine() {
        return settings == null ? null : settings.ruleEngine();
    }

    /**
     * The static enforcement settings, or {@code null} if {@code <settings>} or {@code <staticEnforcement>} was omitted.
     */
    public StaticEnforcementDto staticEnforcement() {
        return settings == null ? null : settings.staticEnforcement();
    }
}
