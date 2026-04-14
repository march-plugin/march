package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ArtifactIdNamingConventionViolationException;
import io.github.march_plugin.core.config.classification.exception.GroupIdNamingConventionViolationException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleConventionValidatorTest {
    private final ModuleConventionValidator validator = new ModuleConventionValidator();

    @Test
    void shouldThrowGroupIdViolationWhenMismatch() {
        final var convention = mock(ModuleConvention.class);
        when(convention.getGroupId()).thenReturn("com.expected");
        final var coords = new ModuleCoordinates("com.actual", "art");

        assertThatThrownBy(() -> validator.validate(convention, k -> k, coords))
                .isInstanceOf(GroupIdNamingConventionViolationException.class);
    }

    @Test
    void shouldThrowArtifactIdViolationWhenMismatch() {
        final var convention = mock(ModuleConvention.class);
        when(convention.getArtifactId()).thenReturn("expected-art");
        final var coords = new ModuleCoordinates("group", "actual-art");

        assertThatThrownBy(() -> validator.validate(convention, k -> k, coords))
                .isInstanceOf(ArtifactIdNamingConventionViolationException.class);
    }
}