package io.github.march_plugin;

import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectModuleRegistryInitializerTest {

    private final ProjectModuleRegistryInitializer initializer = new ProjectModuleRegistryInitializer();

    @TempDir
    private Path tempDir;

    private static MavenProject mavenProject(final String groupId, final String artifactId, final Path baseDir,
                                              final String outputDirectory, final Model originalModel) {
        final var project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn(groupId);
        when(project.getArtifactId()).thenReturn(artifactId);
        when(project.getBasedir()).thenReturn(baseDir.toFile());

        final var build = new Build();
        build.setOutputDirectory(outputDirectory);
        when(project.getBuild()).thenReturn(build);
        when(project.getOriginalModel()).thenReturn(originalModel);
        return project;
    }

    private static Dependency dependency(final String groupId, final String artifactId, final String version,
                                          final String scope, final List<Exclusion> exclusions) {
        final var dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope);
        dependency.setExclusions(exclusions);
        return dependency;
    }

    private static Exclusion exclusion(final String groupId, final String artifactId) {
        final var exclusion = new Exclusion();
        exclusion.setGroupId(groupId);
        exclusion.setArtifactId(artifactId);
        return exclusion;
    }

    @Test
    void shouldMapProjectCoordinatesAndBaseDir() {
        final var project = mavenProject("io.example", "app", tempDir, tempDir.resolve("target/does-not-exist").toString(), new Model());

        final var registry = initializer.initialize(List.of(project));

        final var moduleInfo = registry.getAllProjectModules().get(new ModuleCoordinates("io.example", "app"));
        assertThat(moduleInfo).isNotNull();
        assertThat(moduleInfo.baseDir()).isEqualTo(tempDir);
        assertThat(moduleInfo.dependencies()).isEmpty();
        assertThat(moduleInfo.managedDependencies()).isEmpty();
    }

    @Test
    void shouldMapOutputDirWhenItExists() {
        final var project = mavenProject("io.example", "app", tempDir, tempDir.toString(), new Model());

        final var registry = initializer.initialize(List.of(project));

        final var moduleInfo = registry.getAllProjectModules().get(new ModuleCoordinates("io.example", "app"));
        assertThat(moduleInfo.outputDir()).isEqualTo(tempDir);
    }

    @Test
    void shouldMapNullOutputDirWhenItDoesNotExist() {
        final var project = mavenProject("io.example", "app", tempDir, tempDir.resolve("does-not-exist").toString(), new Model());

        final var registry = initializer.initialize(List.of(project));

        final var moduleInfo = registry.getAllProjectModules().get(new ModuleCoordinates("io.example", "app"));
        assertThat(moduleInfo.outputDir()).isNull();
    }

    @Test
    void shouldMapDependenciesWithVersionScopeAndExclusions() {
        final var model = new Model();
        model.setDependencies(List.of(dependency("io.example", "lib", "1.0.0", "test", List.of(exclusion("io.excluded", "lib")))));
        final var project = mavenProject("io.example", "app", tempDir, tempDir.toString(), model);

        final var registry = initializer.initialize(List.of(project));

        final var moduleInfo = registry.getAllProjectModules().get(new ModuleCoordinates("io.example", "app"));
        assertThat(moduleInfo.dependencies()).hasSize(1);

        final var rawDependency = moduleInfo.dependencies().getFirst();
        assertThat(rawDependency.moduleCoordinates()).isEqualTo(new ModuleCoordinates("io.example", "lib"));
        assertThat(rawDependency.version()).isEqualTo("1.0.0");
        assertThat(rawDependency.scope()).isEqualTo("test");
        assertThat(rawDependency.exclusions()).containsExactly(new ModuleCoordinates("io.excluded", "lib"));
    }

    @Test
    void shouldMapEmptyManagedDependenciesWhenDependencyManagementIsNull() {
        final var model = new Model();
        model.setDependencyManagement(null);
        final var project = mavenProject("io.example", "app", tempDir, tempDir.toString(), model);

        final var registry = initializer.initialize(List.of(project));

        final var moduleInfo = registry.getAllProjectModules().get(new ModuleCoordinates("io.example", "app"));
        assertThat(moduleInfo.managedDependencies()).isEmpty();
    }

    @Test
    void shouldMapManagedDependenciesWhenDependencyManagementIsPresent() {
        final var model = new Model();
        final var dependencyManagement = new DependencyManagement();
        dependencyManagement.setDependencies(List.of(dependency("io.example", "lib", "2.0.0", null, List.of())));
        model.setDependencyManagement(dependencyManagement);
        final var project = mavenProject("io.example", "app", tempDir, tempDir.toString(), model);

        final var registry = initializer.initialize(List.of(project));

        final var moduleInfo = registry.getAllProjectModules().get(new ModuleCoordinates("io.example", "app"));
        assertThat(moduleInfo.managedDependencies()).hasSize(1);
        assertThat(moduleInfo.managedDependencies().getFirst().version()).isEqualTo("2.0.0");
    }

    @Test
    void shouldMapMultipleProjectsIndependently() {
        final var projectA = mavenProject("io.example", "module-a", tempDir, tempDir.toString(), new Model());
        final var projectB = mavenProject("io.example", "module-b", tempDir, tempDir.toString(), new Model());

        final var registry = initializer.initialize(List.of(projectA, projectB));

        assertThat(registry.getAllProjectModules()).containsOnlyKeys(
                new ModuleCoordinates("io.example", "module-a"),
                new ModuleCoordinates("io.example", "module-b"));
    }
}
