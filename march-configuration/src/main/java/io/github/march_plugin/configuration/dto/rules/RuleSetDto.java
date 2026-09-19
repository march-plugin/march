package io.github.march_plugin.configuration.dto.rules;

import java.util.List;

public record RuleSetDto(
        String name,
        RuleConfigurationDto config,
        List<RuleDto> rules
) {

    /**
     * The declared rules.
     */
    public List<RuleDto> rules() {
        return rules == null ? List.of() : rules;
    }
}
