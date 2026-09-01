package io.github.march_plugin.core.config.package_templates.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when a package template is not specified in configuration.
 */
public class PackageTemplateNotFoundException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param packageTemplateName the name of the package template that is not defined in the configuration.
     */
    public PackageTemplateNotFoundException(final String packageTemplateName) {
        super("Package Template '%s' is not defined in March Configuration.".formatted(packageTemplateName));
    }
}