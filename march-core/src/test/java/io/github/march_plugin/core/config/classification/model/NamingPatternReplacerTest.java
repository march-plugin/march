package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.MissingPlaceholderReplacementException;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NamingPatternReplacerTest {
    private final NamingPatternReplacer replacer = new NamingPatternReplacer();

    @Test
    void shouldReturnNullWhenConventionIsNull() {
        assertThat(replacer.replaceString(null, k -> k)).isNull();
    }

    @Test
    void shouldReplacePlaceholders() {
        final var values = Map.of("name", "march", "type", "plugin");
        final var result = replacer.replaceString("io.${name}.${type}", values::get);
        assertThat(result).isEqualTo("io.march.plugin");
    }

    @Test
    void shouldThrowWhenPlaceholderValueIsNull() {
        assertThatThrownBy(() -> replacer.replaceString("${missing}", k -> null))
                .isInstanceOf(MissingPlaceholderReplacementException.class)
                .hasMessageContaining("missing");
    }
}