package io.github.march_plugin.core.config.classification.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleCoordinatesTest {
    @Test
    void verifyModuleCoordinates() {
        final var coords = new ModuleCoordinates("g", "a");
        assertThat(coords.toString()).isEqualTo("g:a");
        assertThat(coords.equals(new ModuleCoordinates("g", "a"))).isTrue();
    }
}