package io.github.march_plugin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleTableFormatTest {

    @Nested
    class Truncate {

        @Test
        void shouldReturnUnchangedTextWhenShorterThanLimit() {
            assertThat(ConsoleTableFormat.truncate("ab", 5)).isEqualTo("ab");
        }

        @Test
        void shouldReturnUnchangedTextWhenExactlyAtLimit() {
            assertThat(ConsoleTableFormat.truncate("abcde", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldTruncateTextLongerThanLimit() {
            assertThat(ConsoleTableFormat.truncate("abcdefgh", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldReturnEmptyStringForNullInput() {
            assertThat(ConsoleTableFormat.truncate(null, 5)).isEmpty();
        }

        @Test
        void shouldDistinguishLabelsThatCollideAtNarrowerWidth() {
            final var wideEnoughToDistinguish = 8;

            assertThat(ConsoleTableFormat.truncate("adapterIn", 5)).isEqualTo(ConsoleTableFormat.truncate("adapterOut", 5));
            assertThat(ConsoleTableFormat.truncate("adapterIn", wideEnoughToDistinguish))
                    .isNotEqualTo(ConsoleTableFormat.truncate("adapterOut", wideEnoughToDistinguish));
        }
    }

    @Nested
    class Center {

        @Test
        void shouldPadShortTextEvenlyOnBothSides() {
            final var evenWidth = 6;

            assertThat(ConsoleTableFormat.center("ok", evenWidth)).isEqualTo("  ok  ");
        }

        @Test
        void shouldFavorRightPaddingWhenPaddingIsOdd() {
            assertThat(ConsoleTableFormat.center("ok", 5)).isEqualTo(" ok  ");
        }

        @Test
        void shouldReturnTextUnchangedWhenExactlyAtWidth() {
            assertThat(ConsoleTableFormat.center("abcde", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldTruncateTextLongerThanWidth() {
            assertThat(ConsoleTableFormat.center("abcdefgh", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldPadEmptyTextToFullWidth() {
            assertThat(ConsoleTableFormat.center("", 4)).isEqualTo("    ");
        }

        @Test
        void shouldTreatNullAsEmptyText() {
            assertThat(ConsoleTableFormat.center(null, 4)).isEqualTo("    ");
        }
    }

    @Nested
    class Lines {

        @Test
        void shouldBuildASolidLineOfTheGivenWidth() {
            assertThat(ConsoleTableFormat.line(5)).isEqualTo("-----");
        }

        @Test
        void shouldBuildADottedLineOfTheGivenWidth() {
            assertThat(ConsoleTableFormat.dottedLine(5)).isEqualTo(".....");
        }
    }
}
