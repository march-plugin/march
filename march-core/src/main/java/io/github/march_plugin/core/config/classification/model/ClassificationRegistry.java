package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.DuplicateClassificationException;
import io.github.march_plugin.core.config.classification.exception.DuplicateModuleException;
import io.github.march_plugin.core.config.classification.exception.DuplicatePackageException;
import io.github.march_plugin.core.config.classification.exception.ModuleNotClassifiedException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClassificationRegistry {

    private final Map<ModuleCoordinates, ClassifiedModule> moduleClassifications;
    private final Map<PackageCoordinates, ClassifiedPackage> packageClassifications;

    private ClassificationRegistry(final Map<ModuleCoordinates, ClassifiedModule> moduleClassifications, final Map<PackageCoordinates, ClassifiedPackage> classifiedPackages) {
        this.moduleClassifications = Map.copyOf(moduleClassifications);
        this.packageClassifications = Map.copyOf(classifiedPackages);
    }

    public Collection<ClassifiedPackage> getAllClassifiedPackages() {
        return packageClassifications.values();
    }

    /**
     * Returns all classified modules of a given type.
     *
     * @param moduleType the typ of the module
     * @return all classified modules of type moduleType
     */
    public Set<ClassifiedModule> getAllModulesOfType(final Class<? extends ClassifiedModule> moduleType) {
        return moduleClassifications.values().stream()
                .filter(moduleType::isInstance)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the classification of a module.
     *
     * @param moduleCoordinates the maven coordinates of the module
     * @return the classification
     */
    public ClassifiedModule getClassifiedModule(final ModuleCoordinates moduleCoordinates) {
        final var moduleClassification = moduleClassifications.get(moduleCoordinates);

        if (moduleClassification != null) {
            return moduleClassification;
        }

        final var virtualModuleClassification = moduleClassifications.values()
                .stream()
                .filter(x -> x instanceof ClassifiedVirtualModuleReference v && v.getExternalCoordinates().equals(moduleCoordinates))
                .findFirst()
                .orElse(null);

        if (virtualModuleClassification != null) {
            return virtualModuleClassification;
        }

        throw new ModuleNotClassifiedException(moduleCoordinates.toString());
    }

    public static class Builder {
        private final Map<ModuleCoordinates, ClassifiedModule> moduleClassifications = new HashMap<>();
        private final Map<PackageCoordinates, ClassifiedPackage> packageClassifications = new HashMap<>();
        private final Map<Classification, String> allClassifications = new HashMap<>();

        /**
         * Adds a classified module to the registry.
         *
         * @param classifiedModule the module to add
         */
        public void addModuleClassification(final ClassifiedModule classifiedModule) {
            if (moduleClassifications.get(classifiedModule.getModuleCoordinates()) != null) {
                throw new DuplicateModuleException(classifiedModule.getModuleCoordinates().toString());
            }

            final var existingClassification = allClassifications.get(classifiedModule.getClassification());

            if (existingClassification != null) {
                throw new DuplicateClassificationException(
                        classifiedModule.getClassification().toString(),
                        existingClassification,
                        classifiedModule.getModuleCoordinates().toString());
            }

            allClassifications.put(classifiedModule.getClassification(), classifiedModule.getModuleCoordinates().toString());
            moduleClassifications.put(classifiedModule.getModuleCoordinates(), classifiedModule);
        }

        /**
         * Adds a classified package to the registry.
         *
         * @param classifiedPackage the package to add
         */
        public void addPackageClassification(final ClassifiedPackage classifiedPackage) {
            if (packageClassifications.get(classifiedPackage.getPackageCoordinates()) != null) {
                throw new DuplicatePackageException(classifiedPackage.getPackageCoordinates().toString());
            }

            final var existingClassification = allClassifications.get(classifiedPackage.getClassification());

            if (existingClassification != null) {
                throw new DuplicateClassificationException(
                        classifiedPackage.getClassification().toString(),
                        existingClassification,
                        classifiedPackage.getPackageCoordinates().toString());
            }

            allClassifications.put(classifiedPackage.getClassification(), classifiedPackage.getPackageCoordinates().toString());
            packageClassifications.put(classifiedPackage.getPackageCoordinates(), classifiedPackage);
        }

        /**
         * Builds the registry.
         * @return the built registry
         */
        public ClassificationRegistry build() {
            return new ClassificationRegistry(moduleClassifications, packageClassifications);
        }
    }
}
