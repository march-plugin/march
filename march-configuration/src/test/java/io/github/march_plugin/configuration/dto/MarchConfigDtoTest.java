package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleSetDto;
import io.github.march_plugin.configuration.dto.rules.RuleStrategyDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarchConfigDtoTest {

    private static MarchConfigDto configOf(final SettingsDto settings, final List<RuleSetDto> rules) {
        return new MarchConfigDto(null, null, null, null, settings, rules);
    }

    @Test
    void shouldReturnEmptyRulesWhenRulesElementOmitted() {
        final var config = configOf(null, null);

        assertThat(config.rules()).isEmpty();
    }

    @Test
    void shouldReturnNullActiveRuleSetWhenSettingsElementOmitted() {
        final var config = configOf(null, null);

        assertThat(config.activeRuleSet()).isNull();
    }

    @Test
    void shouldReturnNullActiveRuleSetWhenActiveRuleSetElementOmitted() {
        final var config = configOf(new SettingsDto(null, null, null, null, null, null), null);

        assertThat(config.activeRuleSet()).isNull();
    }

    @Test
    void shouldReturnNullActiveRuleSetWhenNameDoesNotMatchAnyDeclaredRuleSet() {
        final var ruleSet = new RuleSetDto("default-allow", new RuleConfigurationDto(RuleStrategyDto.DEFAULT_ALLOW, null, null), null);
        final var config = configOf(new SettingsDto("typo", null, null, null, null, null), List.of(ruleSet));

        assertThat(config.activeRuleSet()).isNull();
    }

    @Test
    void shouldResolveActiveRuleSetByName() {
        final var allow = new RuleSetDto("default-allow", new RuleConfigurationDto(RuleStrategyDto.DEFAULT_ALLOW, null, null), null);
        final var deny = new RuleSetDto("default-deny", new RuleConfigurationDto(RuleStrategyDto.DEFAULT_DENY, null, null), null);
        final var config = configOf(new SettingsDto("default-deny", null, null, null, null, null), List.of(allow, deny));

        assertThat(config.activeRuleSet()).isEqualTo(deny);
    }
}
