package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.SettingsDto;
import io.github.march_plugin.core.enforcement.dependencies.StaticEnforcementConfig;

/**
 * Builds a {@link StaticEnforcementConfig} from the static enforcement settings declared in the
 * March configuration, falling back to {@link StaticEnforcementConfig#defaults()} for anything unset.
 */
public class StaticEnforcementConfigInitializer {

    /**
     * Builds the static enforcement config from the given settings.
     *
     * @param settings the settings declared in the March configuration
     * @return the built static enforcement config
     */
    public StaticEnforcementConfig build(final SettingsDto settings) {
        final var defaults = StaticEnforcementConfig.defaults();
        if (settings == null) {
            return defaults;
        }

        return new StaticEnforcementConfig(
                settings.requireManagedVersion() == null ? defaults.requireManagedVersion() : settings.requireManagedVersion(),
                settings.forbidInlineVersion() == null ? defaults.forbidInlineVersion() : settings.forbidInlineVersion(),
                settings.forbidInlineScope() == null ? defaults.forbidInlineScope() : settings.forbidInlineScope(),
                settings.forbidExclusions() == null ? defaults.forbidExclusions() : settings.forbidExclusions(),
                settings.requireVersionProperty() == null ? defaults.requireVersionProperty() : settings.requireVersionProperty()
        );
    }
}
