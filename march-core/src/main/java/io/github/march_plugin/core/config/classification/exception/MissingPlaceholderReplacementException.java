package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a placeholder could not be replaced.
 */
public class MissingPlaceholderReplacementException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param placeHolder the placeHolder that could not be replaced.
     * @param namingConvention the full naming-convention template string the placeholder was found in.
     */
    public MissingPlaceholderReplacementException(final String placeHolder, final String namingConvention) {
        super(("The placeholder '${%s}' in naming convention '%s' could not be replaced: '%s' is not a dimension "
                + "that is classified for this module/package (or any of its ancestors)")
                .formatted(placeHolder, namingConvention, placeHolder));
    }
}