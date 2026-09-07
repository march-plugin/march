package io.github.march_plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarchConfigSchemaValidatorTest {

    @TempDir
    private Path tempDir;

    private static final String VALID_CONFIG = """
            <?xml version="1.0" encoding="UTF-8"?>
            <march>
                <dimensions>
                    <dimension>
                        <name>layer</name>
                        <partitions>
                            <partition><name>service</name></partition>
                            <partition><name>web</name></partition>
                        </partitions>
                    </dimension>
                </dimensions>
                <projectStructure>
                    <modularity dimension="layer" groupId="com.example" artifactId="root"/>
                </projectStructure>
                <packageTemplates/>
                <modules>
                    <module groupId="com.example" artifactId="root"/>
                </modules>
            </march>
            """;

    private Path configFile(final String content) throws IOException {
        final var file = tempDir.resolve("march-config.xml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void shouldAcceptConfigMatchingTheSchema() throws IOException {
        final var file = configFile(VALID_CONFIG);

        assertThatCode(() -> new MarchConfigSchemaValidator().validate(file.toFile()))
                .doesNotThrowAnyException();
    }

    private static final String CONFIG_MISSING_MODULES = """
            <?xml version="1.0" encoding="UTF-8"?>
            <march>
                <dimensions>
                    <dimension>
                        <name>layer</name>
                        <partitions>
                            <partition><name>service</name></partition>
                            <partition><name>web</name></partition>
                        </partitions>
                    </dimension>
                </dimensions>
                <projectStructure>
                    <modularity dimension="layer" groupId="com.example" artifactId="root"/>
                </projectStructure>
                <packageTemplates/>
            </march>
            """;

    @Test
    void shouldRejectConfigMissingARequiredElement() throws IOException {
        final var file = configFile(CONFIG_MISSING_MODULES);

        assertThatThrownBy(() -> new MarchConfigSchemaValidator().validate(file.toFile()))
                .isInstanceOf(MarchConfigSchemaViolationException.class)
                .hasMessageContaining("march-config.xsd");
    }

    @Test
    void shouldRejectConfigWithUnknownElement() throws IOException {
        final var withTypo = VALID_CONFIG.replace("<packageTemplates/>", "<packageTemplatez/>");
        final var file = configFile(withTypo);

        assertThatThrownBy(() -> new MarchConfigSchemaValidator().validate(file.toFile()))
                .isInstanceOf(MarchConfigSchemaViolationException.class);
    }

    @Test
    void shouldRejectMalformedXml() throws IOException {
        final var file = configFile("<march><dimensions>");

        assertThatThrownBy(() -> new MarchConfigSchemaValidator().validate(file.toFile()))
                .isInstanceOf(MarchConfigSchemaViolationException.class);
    }
}
