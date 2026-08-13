package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.MissingPlaceholderReplacementException;

import java.util.function.Function;
import java.util.regex.Pattern;

public final class NamingPatternReplacer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Replaces a naming convention string with classified partitions.
     *
     * @param namingConvention the naming convention containing placeholders
     * @param replacementFunction a function providing the replacement for a certain placeholder
     * @return the replaced naming convention
     *
     */
    public String replaceString(final String namingConvention, final Function<String, String> replacementFunction) {
        if (namingConvention == null) {
            return null;
        }

        final var stringBuilder = new StringBuilder();
        final var matcher = PLACEHOLDER_PATTERN.matcher(namingConvention);
        var lastEnd = 0;

        while (matcher.find()) {
            stringBuilder.append(namingConvention, lastEnd, matcher.start());
            final var replacement = replacementFunction.apply(matcher.group(1));

            if (replacement == null) {
                throw new MissingPlaceholderReplacementException(matcher.group(1), namingConvention);
            }

            stringBuilder.append(replacement);
            lastEnd = matcher.end();
        }

        stringBuilder.append(namingConvention.substring(lastEnd));
        return stringBuilder.toString();
    }
}
