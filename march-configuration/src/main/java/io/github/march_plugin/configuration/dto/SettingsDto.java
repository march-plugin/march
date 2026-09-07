package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;

public record SettingsDto(
        RuleConfigurationDto ruleEngine,
        StaticEnforcementDto staticEnforcement
) {
}
