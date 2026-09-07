package io.github.march_plugin.configuration.dto;

public record StaticEnforcementDto(
        Boolean requireManagedVersion,
        Boolean forbidInlineVersion,
        Boolean forbidInlineScope,
        Boolean forbidExclusions
) {
}
