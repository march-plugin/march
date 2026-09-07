package io.github.march_plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarchModuleMatrixMojoTest {

    @Nested
    class ColumnWidthValidation {

        @Test
        void shouldThrowWhenColumnWidthIsZero() {
            assertThatThrownBy(() -> MarchModuleMatrixMojo.validateColumnWidth(0))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("march.columnWidth");
        }

        @Test
        void shouldThrowWhenColumnWidthIsNegative() {
            final var negativeColumnWidth = -3;

            assertThatThrownBy(() -> MarchModuleMatrixMojo.validateColumnWidth(negativeColumnWidth))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("march.columnWidth");
        }

        @Test
        void shouldAcceptMinimumValidColumnWidthOfOne() {
            assertThatCode(() -> MarchModuleMatrixMojo.validateColumnWidth(1)).doesNotThrowAnyException();
        }
    }
}
