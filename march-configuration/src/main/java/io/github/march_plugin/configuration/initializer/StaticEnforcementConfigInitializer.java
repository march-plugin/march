package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.StaticEnforcementDto;
import io.github.march_plugin.core.enforcement.dependencies.StaticEnforcementConfig;

/**
 * Builds a {@link StaticEnforcementConfig} from the static enforcement settings declared in the
 * March configuration, falling back to {@link StaticEnforcementConfig#defaults()} for anything unset.
 */
public class StaticEnforcementConfigInitializer {

    /**
     * Builds the static enforcement config from the given DTO.
     *
     * @param dto the static enforcement settings declared in the March configuration, or {@code null} if omitted
     * @return the built static enforcement config
     */
    public StaticEnforcementConfig build(final StaticEnforcementDto dto) {
        final var defaults = StaticEnforcementConfig.defaults();
        if (dto == null) {
            return defaults;
        }

        return new StaticEnforcementConfig(
                dto.requireManagedVersion() == null ? defaults.requireManagedVersion() : dto.requireManagedVersion(),
                dto.forbidInlineVersion() == null ? defaults.forbidInlineVersion() : dto.forbidInlineVersion(),
                dto.forbidInlineScope() == null ? defaults.forbidInlineScope() : dto.forbidInlineScope(),
                dto.forbidExclusions() == null ? defaults.forbidExclusions() : dto.forbidExclusions(),
                dto.requireVersionProperty() == null ? defaults.requireVersionProperty() : dto.requireVersionProperty()
        );
    }
}
