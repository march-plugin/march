package io.github.march_plugin.core.enforcement.dependencies;

import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenExclusionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenInlineScopeException;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenInlineVersionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.VersionNotDefinedException;
import io.github.march_plugin.core.project.ProjectModuleRegistry;

/**
 * Enforces that module dependencies are declared through dependency management, without inline versions,
 * scopes, or exclusions.
 */
public class ModuleDependencyEnforcer {

    /**
     * Validates the dependency declarations of all modules in the project.
     *
     * @param projectModuleRegistry the registry of all actually existing maven modules
     * @param registry the registry containing all classifications, used to resolve dependency coordinates
     */
    public void validateDependencyDefinitions(final ProjectModuleRegistry projectModuleRegistry, final ClassificationRegistry registry) {
        for (final var projectModule : projectModuleRegistry.getAllProjectModules().entrySet()) {
            final var sourceCoordinates = projectModule.getKey();

            for (final var dependency : projectModule.getValue().managedDependencies()) {
                if (dependency.version() == null) {
                    throw new VersionNotDefinedException(sourceCoordinates, new ModuleCoordinates(dependency.moduleCoordinates().getGroupId(), dependency.moduleCoordinates().getArtifactId()));
                }
            }

            final var source = registry.getClassifiedModule(sourceCoordinates);
            for (final var dep : projectModule.getValue().dependencies()) {
                final var dependencyCoordinates = new ModuleCoordinates(dep.moduleCoordinates().getGroupId(), dep.moduleCoordinates().getArtifactId());
                final var target = registry.getClassifiedModule(dependencyCoordinates);


                if (dep.version() != null) {
                    throw new ForbiddenInlineVersionException(source.getModuleCoordinates(), dep.version(), dependencyCoordinates);
                }

                if (dep.scope() != null) {
                    throw new ForbiddenInlineScopeException(source.getModuleCoordinates(), dep.scope(), dependencyCoordinates);
                }

                for (final var exclusion : dep.exclusions()) {
                    final var excludedModule = new ModuleCoordinates(exclusion.getGroupId(), exclusion.getArtifactId());
                    final var dependencyDescription = source.getModuleCoordinates() + " -> " + target.getModuleCoordinates();
                    throw new ForbiddenExclusionException(dependencyDescription, excludedModule.toString());
                }
            }

        }
    }
}
