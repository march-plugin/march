package io.github.march_plugin.configuration.dto.rules;

public record RuleDto(
        String description,
        String definition,
        ValidationScopeDto scope
) {}