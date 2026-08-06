package io.github.march_plugin.configuration.dto.rules;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RuleStrategyDto {
    @JsonProperty("DEFAULT-DENY")
    DEFAULT_DENY,

    @JsonProperty("DEFAULT-ALLOW")
    DEFAULT_ALLOW
}