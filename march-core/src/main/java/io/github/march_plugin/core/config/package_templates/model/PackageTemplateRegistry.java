package io.github.march_plugin.core.config.package_templates.model;

import io.github.march_plugin.core.config.package_templates.exception.DuplicationPackageTemplateDefinitionException;
import io.github.march_plugin.core.config.package_templates.exception.PackageTemplateNotFoundException;

import java.util.HashMap;
import java.util.Map;

public final class PackageTemplateRegistry {

    private final Map<String, JPackage> packageTemplates;

    private PackageTemplateRegistry(final Map<String, JPackage> packageTemplates) {
        this.packageTemplates = Map.copyOf(packageTemplates);
    }

    /**
     * Finds a package template by name.
     *
     * @param templateName the name of the package template
     * @return the root package of the package template
     */
    public JPackage getPackageTemplate(final String templateName) {
        final var template = packageTemplates.get(templateName);
        if (template == null) {
            throw new PackageTemplateNotFoundException(templateName);
        }
        return template;
    }

    public static class Builder {
        private final Map<String, JPackage> packageTemplates = new HashMap<>();

        /**
         * Adds a package template to the registry.
         *
         * @param templateName the name of the template
         * @param rootPackage the root package of the template
         */
        public void addPackageTemplate(final String templateName, final JPackage rootPackage) {
            if (packageTemplates.get(templateName) != null) {
                throw new DuplicationPackageTemplateDefinitionException(templateName);
            }

            packageTemplates.put(templateName, rootPackage);
        }

        /**
         * Builds the registry.
         * @return the built registry
         */
        public PackageTemplateRegistry build() {
            return new PackageTemplateRegistry(packageTemplates);
        }
    }
}