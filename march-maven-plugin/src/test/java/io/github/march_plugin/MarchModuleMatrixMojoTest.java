package io.github.march_plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Nested
    class Center {

        @Test
        void shouldPadShortTextEvenlyOnBothSides() {
            final var evenWidth = 6;

            assertThat(MarchModuleMatrixMojo.center("ok", evenWidth)).isEqualTo("  ok  ");
        }

        @Test
        void shouldFavorRightPaddingWhenPaddingIsOdd() {
            assertThat(MarchModuleMatrixMojo.center("ok", 5)).isEqualTo(" ok  ");
        }

        @Test
        void shouldReturnTextUnchangedWhenExactlyAtWidth() {
            assertThat(MarchModuleMatrixMojo.center("abcde", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldTruncateTextLongerThanWidth() {
            assertThat(MarchModuleMatrixMojo.center("abcdefgh", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldPadEmptyTextToFullWidth() {
            assertThat(MarchModuleMatrixMojo.center("", 4)).isEqualTo("    ");
        }
    }
}
