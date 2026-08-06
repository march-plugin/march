package io.github.march_plugin.configuration.dto.rules;

import io.github.march_plugin.core.config.rules.model.Rule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationScopeDtoTest {

    @ParameterizedTest
    @CsvSource({
        "global, GLOBAL",
        "module_only, MODULE_ONLY",
        "package_only, PACKAGE_ONLY"
    })
    void shouldMapToMatchingRuleScope(final ValidationScopeDto scopeDto, final Rule.RuleScope expected) {
        assertThat(scopeDto.toRuleScope()).isEqualTo(expected);
    }
}
