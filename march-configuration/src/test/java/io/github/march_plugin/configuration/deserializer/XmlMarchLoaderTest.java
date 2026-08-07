package io.github.march_plugin.configuration.deserializer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlMarchLoaderTest {

    private final XmlMarchLoader loader = new XmlMarchLoader();

    private static ByteArrayInputStream xml(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldDeserializeEmptyConfig() {
        final var dto = loader.deserializeMarchConfigDto(xml("<config/>"));

        assertThat(dto.dimensions()).isNull();
        assertThat(dto.projectStructure()).isNull();
        assertThat(dto.packageTemplates()).isNull();
        assertThat(dto.modules()).isNull();
        assertThat(dto.rules()).isNull();
    }

    @Test
    void shouldDeserializePackageTemplatesWithNestedPackages() {
        final var dto = loader.deserializeMarchConfigDto(xml("""
                <config>
                    <packageTemplates>
                        <packageTemplate>
                            <name>standard</name>
                            <jpackage>
                                <name>main</name>
                                <partition>service</partition>
                                <optional>true</optional>
                            </jpackage>
                        </packageTemplate>
                    </packageTemplates>
                </config>
                """));

        assertThat(dto.packageTemplates()).isNotNull();
        assertThat(dto.packageTemplates().packageTemplate()).hasSize(1);

        final var template = dto.packageTemplates().packageTemplate().getFirst();
        assertThat(template.name()).isEqualTo("standard");
        assertThat(template.jpackage()).hasSize(1);

        final var jpackage = template.jpackage().getFirst();
        assertThat(jpackage.name()).isEqualTo("main");
        assertThat(jpackage.partition()).isEqualTo("service");
        assertThat(jpackage.optional()).isTrue();
    }

    @Test
    void shouldThrowWhenInputIsMalformed() {
        assertThatThrownBy(() -> loader.deserializeMarchConfigDto(xml("not xml at all")))
                .isInstanceOf(RuntimeException.class);
    }
}
