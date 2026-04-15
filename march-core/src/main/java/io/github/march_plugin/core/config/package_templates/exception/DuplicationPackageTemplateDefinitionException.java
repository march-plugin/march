package io.github.march_plugin.core.config.package_templates.exception;

/**
 * Thrown when a package template with the same name is already defined in the configuration.
 */
public class DuplicationPackageTemplateDefinitionException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param packageTemplateName the package template that is already defined.
     */
    public DuplicationPackageTemplateDefinitionException(final String packageTemplateName) {
        super("Package Template '%s' is already defined in March Configuration.".formatted(packageTemplateName));
    }
}