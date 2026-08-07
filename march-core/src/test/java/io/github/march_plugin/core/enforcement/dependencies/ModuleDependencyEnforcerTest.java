package io.github.march_plugin.core.enforcement.dependencies;

import io.github.march_plugin.core.config.classification.exception.ModuleNotClassifiedException;
import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedModule;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenExclusionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenInlineScopeException;
import io.github.march_plugin.core.enforcement.dependencies.exception.ForbiddenInlineVersionException;
import io.github.march_plugin.core.enforcement.dependencies.exception.VersionNotDefinedException;
import io.github.march_plugin.core.project.ProjectModuleRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleDependencyEnforcerTest {

    private final ModuleDependencyEnforcer enforcer = new ModuleDependencyEnforcer();

    private static ModuleCoordinates coords(final String groupId, final String artifactId) {
        return new ModuleCoordinates(groupId, artifactId);
    }

    private static ProjectModuleRegistry.ProjectModuleInfo.RawDependency rawDependency(
            final ModuleCoordinates coordinates, final String version, final String scope, final Set<ModuleCoordinates> exclusions) {
        return new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(coordinates, version, scope, exclusions);
    }

    private static ProjectModuleRegistry registryWith(final ModuleCoordinates moduleCoordinates,
                                                        final List<ProjectModuleRegistry.ProjectModuleInfo.RawDependency> dependencies,
                                                        final List<ProjectModuleRegistry.ProjectModuleInfo.RawDependency> managedDependencies) {
        final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(Path.of("."), null, dependencies, managedDependencies);
        return new ProjectModuleRegistry(Map.of(moduleCoordinates, moduleInfo));
    }

    private static ClassificationRegistry classificationRegistryKnowing(final ModuleCoordinates... coordinates) {
        final var builder = new ClassificationRegistry.Builder();
        for (final var coordinate : coordinates) {
            final var module = mock(ClassifiedModule.class);
            when(module.getModuleCoordinates()).thenReturn(coordinate);
            when(module.getClassification()).thenReturn(mock(Classification.class));
            builder.addModuleClassification(module);
        }
        return builder.build();
    }

    @Test
    void shouldPassWhenNoDependenciesDeclared() {
        final var source = coords("io.example", "app");
        final var projectModuleRegistry = registryWith(source, List.of(), List.of());
        final var classificationRegistry = classificationRegistryKnowing(source);

        assertThatCode(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPassWhenDependencyIsCleanlyManaged() {
        final var source = coords("io.example", "app");
        final var target = coords("io.example", "lib");
        final var dependency = rawDependency(target, null, null, Set.of());
        final var managedDependency = rawDependency(target, "1.0.0", null, Set.of());

        final var projectModuleRegistry = registryWith(source, List.of(dependency), List.of(managedDependency));
        final var classificationRegistry = classificationRegistryKnowing(source, target);

        assertThatCode(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenManagedDependencyHasNoVersion() {
        final var source = coords("io.example", "app");
        final var target = coords("io.example", "lib");
        final var managedDependency = rawDependency(target, null, null, Set.of());

        final var projectModuleRegistry = registryWith(source, List.of(), List.of(managedDependency));
        final var classificationRegistry = classificationRegistryKnowing(source);

        assertThatThrownBy(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .isInstanceOf(VersionNotDefinedException.class);
    }

    @Test
    void shouldThrowWhenDependencyDefinesInlineVersion() {
        final var source = coords("io.example", "app");
        final var target = coords("io.example", "lib");
        final var dependency = rawDependency(target, "1.0.0", null, Set.of());

        final var projectModuleRegistry = registryWith(source, List.of(dependency), List.of());
        final var classificationRegistry = classificationRegistryKnowing(source, target);

        assertThatThrownBy(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .isInstanceOf(ForbiddenInlineVersionException.class);
    }

    @Test
    void shouldThrowWhenDependencyDefinesInlineScope() {
        final var source = coords("io.example", "app");
        final var target = coords("io.example", "lib");
        final var dependency = rawDependency(target, null, "test", Set.of());

        final var projectModuleRegistry = registryWith(source, List.of(dependency), List.of());
        final var classificationRegistry = classificationRegistryKnowing(source, target);

        assertThatThrownBy(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .isInstanceOf(ForbiddenInlineScopeException.class);
    }

    @Test
    void shouldThrowWhenDependencyDefinesExclusion() {
        final var source = coords("io.example", "app");
        final var target = coords("io.example", "lib");
        final var excluded = coords("io.example", "transitive");
        final var dependency = rawDependency(target, null, null, Set.of(excluded));

        final var projectModuleRegistry = registryWith(source, List.of(dependency), List.of());
        final var classificationRegistry = classificationRegistryKnowing(source, target);

        assertThatThrownBy(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .isInstanceOf(ForbiddenExclusionException.class);
    }

    @Test
    void shouldThrowWhenDependencyIsNotClassified() {
        final var source = coords("io.example", "app");
        final var target = coords("io.example", "unclassified");
        final var dependency = rawDependency(target, null, null, Set.of());

        final var projectModuleRegistry = registryWith(source, List.of(dependency), List.of());
        final var classificationRegistry = classificationRegistryKnowing(source);

        assertThatThrownBy(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .isInstanceOf(ModuleNotClassifiedException.class);
    }

    @Test
    void shouldThrowWhenSourceModuleIsNotClassified() {
        final var source = coords("io.example", "unclassified-app");
        final var projectModuleRegistry = registryWith(source, List.of(), List.of());
        final var classificationRegistry = classificationRegistryKnowing();

        assertThatThrownBy(() -> enforcer.validateDependencyDefinitions(projectModuleRegistry, classificationRegistry))
                .isInstanceOf(ModuleNotClassifiedException.class);
    }
}
