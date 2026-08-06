package io.github.march_plugin.configuration.dto.rules;

import java.util.List;

public record RulesDto(
        List<RuleDto> rules,
        RuleConfigurationDto configuration
) {
}
