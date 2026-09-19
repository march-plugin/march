package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.SettingsDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaticEnforcementConfigInitializerTest {

    private final StaticEnforcementConfigInitializer initializer = new StaticEnforcementConfigInitializer();

    @Test
    void shouldUseDefaultsWhenSettingsIsNull() {
        final var config = initializer.build(null);

        assertThat(config.requireManagedVersion()).isTrue();
        assertThat(config.forbidInlineVersion()).isTrue();
        assertThat(config.forbidInlineScope()).isTrue();
        assertThat(config.forbidExclusions()).isFalse();
        assertThat(config.requireVersionProperty()).isTrue();
    }

    @Test
    void shouldUseDefaultsForFieldsLeftUnset() {
        final var settings = new SettingsDto(null, null, null, null, null, null);

        final var config = initializer.build(settings);

        assertThat(config.requireManagedVersion()).isTrue();
        assertThat(config.forbidInlineVersion()).isTrue();
        assertThat(config.forbidInlineScope()).isTrue();
        assertThat(config.forbidExclusions()).isFalse();
        assertThat(config.requireVersionProperty()).isTrue();
    }

    @Test
    void shouldOverrideDefaultsWithExplicitValues() {
        final var settings = new SettingsDto(null, false, false, false, true, false);

        final var config = initializer.build(settings);

        assertThat(config.requireManagedVersion()).isFalse();
        assertThat(config.forbidInlineVersion()).isFalse();
        assertThat(config.forbidInlineScope()).isFalse();
        assertThat(config.forbidExclusions()).isTrue();
        assertThat(config.requireVersionProperty()).isFalse();
    }

    @Test
    void shouldOverrideOnlyTheFieldsExplicitlySet() {
        final var settings = new SettingsDto(null, false, null, null, true, null);

        final var config = initializer.build(settings);

        assertThat(config.requireManagedVersion()).isFalse();
        assertThat(config.forbidInlineVersion()).isTrue();
        assertThat(config.forbidInlineScope()).isTrue();
        assertThat(config.forbidExclusions()).isTrue();
        assertThat(config.requireVersionProperty()).isTrue();
    }
}
