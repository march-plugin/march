package io.github.march_plugin.configuration.deserializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarchConfigFileReaderTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldReadAndDeserializeValidConfigFile() throws IOException {
        final var configFile = tempDir.resolve("march-config.xml").toFile();
        Files.writeString(configFile.toPath(), "<config/>", StandardCharsets.UTF_8);

        final var dto = new MarchConfigFileReader(configFile).readConfig();

        assertThat(dto).isNotNull();
        assertThat(dto.dimensions()).isNull();
    }

    @Test
    void shouldThrowRuntimeExceptionWhenFileDoesNotExist() {
        final var missingFile = new File(tempDir.toFile(), "does-not-exist.xml");

        assertThatThrownBy(() -> new MarchConfigFileReader(missingFile).readConfig())
                .isInstanceOf(RuntimeException.class);
    }
}
