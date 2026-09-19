package io.github.march_plugin.configuration.dto;

public record SettingsDto(
        String activeRuleSet,
        Boolean requireManagedVersion,
        Boolean forbidInlineVersion,
        Boolean forbidInlineScope,
        Boolean forbidExclusions,
        Boolean requireVersionProperty
) {
}
