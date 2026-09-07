package io.github.march_plugin;

/**
 * Formatting helpers shared by the plugin's ASCII table output ({@code march:matrix}, {@code march:module-matrix}).
 */
final class ConsoleTableFormat {

    private ConsoleTableFormat() {
    }

    /**
     * Truncates text to at most {@code len} characters. A null input is treated as empty.
     */
    static String truncate(final String text, final int len) {
        final var safeText = text == null ? "" : text;
        return safeText.length() > len ? safeText.substring(0, len) : safeText;
    }

    /**
     * Centers text within a fixed width, padding with spaces. A null input is treated as empty.
     * Text at least as long as {@code width} is truncated to exactly {@code width} characters.
     */
    static String center(final String text, final int width) {
        final var safeText = text == null ? "" : text;
        if (safeText.length() >= width) {
            return safeText.substring(0, width);
        }

        final var padding = width - safeText.length();
        final var leftPadding = padding / 2;
        final var rightPadding = padding - leftPadding;

        return " ".repeat(leftPadding) + safeText + " ".repeat(rightPadding);
    }

    /**
     * Builds a solid horizontal line of the given width.
     */
    static String line(final int width) {
        return "-".repeat(width);
    }

    /**
     * Builds a dotted horizontal line of the given width.
     */
    static String dottedLine(final int width) {
        return ".".repeat(width);
    }
}
