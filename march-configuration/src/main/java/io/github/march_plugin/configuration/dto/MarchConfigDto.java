package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.classification.ModulesDto;
import io.github.march_plugin.configuration.dto.dimensions.DimensionDto;
import io.github.march_plugin.configuration.dto.modularity.ProjectStructureDto;
import io.github.march_plugin.configuration.dto.package_templates.PackageTemplatesDto;
import io.github.march_plugin.configuration.dto.rules.RuleSetDto;

import java.util.List;

public record MarchConfigDto(
        List<DimensionDto> dimensions,
        ProjectStructureDto projectStructure,
        PackageTemplatesDto packageTemplates,
        ModulesDto modules,
        SettingsDto settings,
        List<RuleSetDto> rules
) {

    /**
     * The declared rule sets, or an empty list if {@code <rules>} was omitted entirely.
     */
    public List<RuleSetDto> rules() {
        return rules == null ? List.of() : rules;
    }

    /**
     * The active rule set configured in settings.
     */
    public RuleSetDto activeRuleSet() {
        final var activeRuleSetName = settings == null ? null : settings.activeRuleSet();
        if (activeRuleSetName == null) {
            return null;
        }
        return rules().stream().filter(ruleSet -> activeRuleSetName.equals(ruleSet.name())).findFirst().orElse(null);
    }
}
