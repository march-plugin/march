package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.package_templates.JPackageDto;
import io.github.march_plugin.configuration.dto.package_templates.PackageTemplatesDto;
import io.github.march_plugin.core.config.package_templates.model.JPackage;
import io.github.march_plugin.core.config.package_templates.model.PackageTemplateRegistry;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

import java.util.ArrayList;

/**
 * Builds a {@link PackageTemplateRegistry} from the package templates declared in the March configuration.
 */
public class PackageTemplateRegistryInitializer {

    private final PackageTemplateRegistry.Builder registryBuilder;

    /**
     * Constructs the initializer.
     */
    public PackageTemplateRegistryInitializer() {
        registryBuilder = new PackageTemplateRegistry.Builder();
    }

    /**
     * Builds the package template registry from the given package template configuration.
     *
     * @param packageTemplatesDto the package templates declared in the March configuration
     * @return the package template registry populated with the configured templates
     */
    public PackageTemplateRegistry build(final PackageTemplatesDto packageTemplatesDto) {
        registerPackageTemplates(packageTemplatesDto);
        return registryBuilder.build();
    }

    private void registerPackageTemplates(final PackageTemplatesDto packageTemplatesDto) {
        if (packageTemplatesDto.packageTemplate() != null) {
            for (final var packageTemplateDto : packageTemplatesDto.packageTemplate()) {
                final var packageTemplate = new JPackage(null, null, false, new ArrayList<>());

                if (packageTemplateDto.jpackage() != null) {
                    for (final var jPackageDto : packageTemplateDto.jpackage()) {
                        registerPackageTemplatePackage(packageTemplate, jPackageDto);
                    }
                }

                registryBuilder.addPackageTemplate(packageTemplateDto.name(), packageTemplate);
            }
        }
    }

    private void registerPackageTemplatePackage(final JPackage parent, final JPackageDto childJPackageDto) {
        final var packageHierarchy = PackageHierarchy.buildChild(parent.packageHierarchy(), childJPackageDto.name());
        final var childJPackage = new JPackage(packageHierarchy, childJPackageDto.partition(), childJPackageDto.optional() != null && childJPackageDto.optional(), new ArrayList<>());
        parent.children().add(childJPackage);

        if (childJPackageDto.jpackage() != null) {
            for (final var jPackageDto : childJPackageDto.jpackage()) {
                registerPackageTemplatePackage(childJPackage, jPackageDto);
            }
        }
    }
}
