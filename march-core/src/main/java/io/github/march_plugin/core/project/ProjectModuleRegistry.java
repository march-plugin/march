package io.github.march_plugin.core.project;

import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registry for all actually existing Maven Modules.
 */
public class ProjectModuleRegistry {

    private final Map<ModuleCoordinates, ProjectModuleInfo> projectModuleInfoMap;

    /**
     * Constructs the registry.
     *
     * @param projectModuleInfoMap maps all maven modules to their additional information
     */
    public ProjectModuleRegistry(final Map<ModuleCoordinates, ProjectModuleInfo> projectModuleInfoMap) {
        this.projectModuleInfoMap = Map.copyOf(projectModuleInfoMap);
    }

    public Map<ModuleCoordinates, ProjectModuleInfo> getAllProjectModules() {
        return projectModuleInfoMap;
    }

    public List<Path> getAllOutputDirs() {
        return projectModuleInfoMap.values().stream()
                .map(ProjectModuleInfo::outputDir)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Resolves the maven dependencies of all modules in the project to dependencies between their classifications.
     *
     * @param registry the registry containing all classifications
     * @return the dependencies between the classifications of all modules in the project
     */
    public Set<MavenDependency> getDependencies(final ClassificationRegistry registry) {
        final var moduleDependencies = new HashSet<MavenDependency>();

        for (final var moduleEntry : getAllProjectModules().entrySet()) {
            final var source = registry.getClassifiedModule(moduleEntry.getKey());

            for (final var dependency : moduleEntry.getValue().dependencies()) {
                final var target = registry.getClassifiedModule(dependency.moduleCoordinates());
                final var dependencyDescription = source.getModuleCoordinates() + " -> " + target.getModuleCoordinates();
                moduleDependencies.add(new MavenDependency(source.getClassification(), target.getClassification(), dependencyDescription));
            }
        }
        return moduleDependencies;
    }

    /**
     * Holds the physical information about a module found in the project.
     *
     * @param baseDir the base directory of the module
     * @param outputDir the build output directory of the module, {@code null} if the module has not been built
     * @param dependencies the direct dependencies declared by the module
     * @param managedDependencies the dependencies declared in the module's {@code dependencyManagement}
     */
    public record ProjectModuleInfo(Path baseDir,
                                    Path outputDir,
                                    List<RawDependency> dependencies,
                                    List<RawDependency> managedDependencies) {
        public record RawDependency(
                ModuleCoordinates moduleCoordinates,
                String version,
                String scope,
                Set<ModuleCoordinates> exclusions
        ) {}
    }

}
