package io.github.march_plugin.core.enforcement.dependencies;

import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedModule;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenExclusionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenInlineScopeException;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenInlineVersionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.HardcodedVersionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.VersionNotDefinedException;
import io.github.march_plugin.core.project.ProjectModuleRegistry;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enforces that module dependencies are declared through dependency management, without inline versions,
 * scopes, or exclusions.
 */
public class ModuleDependencyEnforcer {

    private static final Pattern PROPERTY_REFERENCE = Pattern.compile("\\$\\{[^}]+}");

    private final StaticEnforcementConfig config;

    /**
     * Constructs the enforcer.
     *
     * @param config configures which checks below are enabled
     */
    public ModuleDependencyEnforcer(final StaticEnforcementConfig config) {
        this.config = config;
    }

    /**
     * Validates the dependency declarations of all modules in the project.
     *
     * @param projectModuleRegistry the registry of all actually existing maven modules
     * @param registry the registry containing all classifications, used to resolve dependency coordinates
     */
    public void validateDependencyDefinitions(final ProjectModuleRegistry projectModuleRegistry, final ClassificationRegistry registry) {
        for (final var projectModule : projectModuleRegistry.getAllProjectModules().entrySet()) {
            final var sourceCoordinates = projectModule.getKey();

            validateManagedDependencies(sourceCoordinates, projectModule.getValue().managedDependencies());

            final var source = registry.getClassifiedModule(sourceCoordinates);
            for (final var dep : projectModule.getValue().dependencies()) {
                validateDeclaredDependency(source, dep, registry);
            }
        }
    }

    private void validateManagedDependencies(final ModuleCoordinates sourceCoordinates,
                                              final List<ProjectModuleRegistry.ProjectModuleInfo.RawDependency> managedDependencies) {
        for (final var dependency : managedDependencies) {
            if (config.requireManagedVersion() && dependency.version() == null) {
                throw new VersionNotDefinedException(sourceCoordinates, dependency.moduleCoordinates());
            }

            if (config.requireVersionProperty() && dependency.version() != null && !isPropertyReference(dependency.version())) {
                throw new HardcodedVersionException(sourceCoordinates, dependency.version(), dependency.moduleCoordinates());
            }
        }
    }

    private void validateDeclaredDependency(final ClassifiedModule source, final ProjectModuleRegistry.ProjectModuleInfo.RawDependency dep,
                                             final ClassificationRegistry registry) {
        final var dependencyCoordinates = dep.moduleCoordinates();
        final var target = registry.getClassifiedModule(dependencyCoordinates);

        if (config.forbidInlineVersion() && dep.version() != null) {
            throw new ForbiddenInlineVersionException(source.getModuleCoordinates(), dep.version(), dependencyCoordinates);
        }

        if (config.requireVersionProperty() && dep.version() != null && !isPropertyReference(dep.version())) {
            throw new HardcodedVersionException(source.getModuleCoordinates(), dep.version(), dependencyCoordinates);
        }

        if (config.forbidInlineScope() && dep.scope() != null) {
            throw new ForbiddenInlineScopeException(source.getModuleCoordinates(), dep.scope(), dependencyCoordinates);
        }

        if (config.forbidExclusions()) {
            for (final var exclusion : dep.exclusions()) {
                final var excludedModule = new ModuleCoordinates(exclusion.getGroupId(), exclusion.getArtifactId());
                final var dependencyDescription = source.getModuleCoordinates() + " -> " + target.getModuleCoordinates();
                throw new ForbiddenExclusionException(dependencyDescription, excludedModule.toString());
            }
        }
    }

    private static boolean isPropertyReference(final String version) {
        return PROPERTY_REFERENCE.matcher(version).matches();
    }
}
