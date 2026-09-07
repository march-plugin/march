package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;
import io.github.march_plugin.configuration.dto.rules.RuleStrategyDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarchConfigDtoTest {

    private static MarchConfigDto configOf(final SettingsDto settings, final List<RuleDto> rules) {
        return new MarchConfigDto(null, null, null, null, settings, rules);
    }

    @Test
    void shouldReturnEmptyRulesWhenRulesElementOmitted() {
        final var config = configOf(null, null);

        assertThat(config.rules()).isEmpty();
    }

    @Test
    void shouldReturnNullRuleEngineWhenSettingsElementOmitted() {
        final var config = configOf(null, null);

        assertThat(config.ruleEngine()).isNull();
    }

    @Test
    void shouldReturnNullStaticEnforcementWhenSettingsElementOmitted() {
        final var config = configOf(null, null);

        assertThat(config.staticEnforcement()).isNull();
    }

    @Test
    void shouldReturnNullRuleEngineWhenRuleEngineElementOmitted() {
        final var config = configOf(new SettingsDto(null, null), null);

        assertThat(config.ruleEngine()).isNull();
    }

    @Test
    void shouldReturnRuleEngineWhenSettingsPresent() {
        final var ruleEngine = new RuleConfigurationDto(RuleStrategyDto.DEFAULT_ALLOW, null);
        final var config = configOf(new SettingsDto(ruleEngine, null), null);

        assertThat(config.ruleEngine()).isEqualTo(ruleEngine);
    }

    @Test
    void shouldReturnStaticEnforcementWhenSettingsPresent() {
        final var staticEnforcement = new StaticEnforcementDto(true, true, true, true, true);
        final var config = configOf(new SettingsDto(null, staticEnforcement), null);

        assertThat(config.staticEnforcement()).isEqualTo(staticEnforcement);
    }
}
