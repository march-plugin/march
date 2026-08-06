package io.github.march_plugin.core.project;

import io.github.march_plugin.core.config.classification.exception.ModuleNotClassifiedException;
import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedModule;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectModuleRegistryTest {

    private static final Path BASE_DIR = Path.of("base");
    private static final Path OUTPUT_DIR = Path.of("target", "classes");

    @Nested
    class OutputDirs {

        @Test
        void shouldReturnEmptyListForEmptyRegistry() {
            final var registry = new ProjectModuleRegistry(Map.of());

            assertThat(registry.getAllOutputDirs()).isEmpty();
        }

        @Test
        void shouldCollectAllNonNullOutputDirs() {
            final var moduleA = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, OUTPUT_DIR, List.of(), List.of());
            final var moduleB = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(), List.of());

            final var registry = new ProjectModuleRegistry(Map.of(
                    new ModuleCoordinates("io.example", "a"), moduleA,
                    new ModuleCoordinates("io.example", "b"), moduleB));

            assertThat(registry.getAllOutputDirs())
                    .hasSize(1)
                    .containsExactly(OUTPUT_DIR);
        }

        @Test
        void shouldReturnEmptyListWhenAllOutputDirsAreNull() {
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(new ModuleCoordinates("io.example", "a"), moduleInfo));

            assertThat(registry.getAllOutputDirs()).isEmpty();
        }
    }

    @Nested
    class Dependencies {

        @Test
        void shouldReturnEmptySetWhenNoModulesExist() {
            final var registry = new ProjectModuleRegistry(Map.of());
            final var classificationRegistry = mock(ClassificationRegistry.class);

            assertThat(registry.getDependencies(classificationRegistry)).isEmpty();
        }

        @Test
        void shouldReturnEmptySetWhenModuleHasNoDependencies() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(coordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            stubModule(classificationRegistry, coordinates);

            assertThat(registry.getDependencies(classificationRegistry)).isEmpty();
        }

        @Test
        void shouldResolveSingleDependencyToClassificationPair() {
            final var sourceCoordinates = new ModuleCoordinates("io.example", "app");
            final var targetCoordinates = new ModuleCoordinates("io.example", "lib");
            final var dependency = new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(targetCoordinates, "1.0", "compile", Set.of());
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(dependency), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(sourceCoordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            final var sourceClassification = mock(Classification.class);
            final var targetClassification = mock(Classification.class);
            stubModule(classificationRegistry, sourceCoordinates, sourceClassification);
            stubModule(classificationRegistry, targetCoordinates, targetClassification);

            final var dependencies = registry.getDependencies(classificationRegistry);

            assertThat(dependencies).hasSize(1);
            final var mavenDependency = dependencies.iterator().next();
            assertThat(mavenDependency.source()).isEqualTo(sourceClassification);
            assertThat(mavenDependency.target()).isEqualTo(targetClassification);
            assertThat(mavenDependency.description()).isEqualTo("io.example:app -> io.example:lib");
        }

        @Test
        void shouldResolveMultipleDependenciesOfTheSameModule() {
            final var sourceCoordinates = new ModuleCoordinates("io.example", "app");
            final var targetCoordinates1 = new ModuleCoordinates("io.example", "lib1");
            final var targetCoordinates2 = new ModuleCoordinates("io.example", "lib2");
            final var dependency1 = new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(targetCoordinates1, null, null, Set.of());
            final var dependency2 = new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(targetCoordinates2, null, null, Set.of());
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(dependency1, dependency2), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(sourceCoordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            stubModule(classificationRegistry, sourceCoordinates);
            stubModule(classificationRegistry, targetCoordinates1);
            stubModule(classificationRegistry, targetCoordinates2);

            assertThat(registry.getDependencies(classificationRegistry)).hasSize(2);
        }

        @Test
        void shouldIgnoreManagedDependencies() {
            final var sourceCoordinates = new ModuleCoordinates("io.example", "app");
            final var targetCoordinates = new ModuleCoordinates("io.example", "lib");
            final var managedDependency = new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(targetCoordinates, "1.0", null, Set.of());
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(), List.of(managedDependency));
            final var registry = new ProjectModuleRegistry(Map.of(sourceCoordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            stubModule(classificationRegistry, sourceCoordinates);

            assertThat(registry.getDependencies(classificationRegistry)).isEmpty();
        }

        @Test
        void shouldCollapseDuplicateDependencyDeclarations() {
            final var sourceCoordinates = new ModuleCoordinates("io.example", "app");
            final var targetCoordinates = new ModuleCoordinates("io.example", "lib");
            final var dependency = new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(targetCoordinates, null, null, Set.of());
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(dependency, dependency), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(sourceCoordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            stubModule(classificationRegistry, sourceCoordinates);
            stubModule(classificationRegistry, targetCoordinates);

            assertThat(registry.getDependencies(classificationRegistry)).hasSize(1);
        }

        @Test
        void shouldThrowWhenSourceModuleIsNotClassified() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(coordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            when(classificationRegistry.getClassifiedModule(coordinates)).thenThrow(new ModuleNotClassifiedException(coordinates.toString()));

            assertThatThrownBy(() -> registry.getDependencies(classificationRegistry))
                    .isInstanceOf(ModuleNotClassifiedException.class);
        }

        @Test
        void shouldThrowWhenDependencyTargetIsNotClassified() {
            final var sourceCoordinates = new ModuleCoordinates("io.example", "app");
            final var targetCoordinates = new ModuleCoordinates("io.example", "unclassified-lib");
            final var dependency = new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(targetCoordinates, null, null, Set.of());
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(BASE_DIR, null, List.of(dependency), List.of());
            final var registry = new ProjectModuleRegistry(Map.of(sourceCoordinates, moduleInfo));

            final var classificationRegistry = mock(ClassificationRegistry.class);
            stubModule(classificationRegistry, sourceCoordinates);
            when(classificationRegistry.getClassifiedModule(targetCoordinates)).thenThrow(new ModuleNotClassifiedException(targetCoordinates.toString()));

            assertThatThrownBy(() -> registry.getDependencies(classificationRegistry))
                    .isInstanceOf(ModuleNotClassifiedException.class);
        }

        private void stubModule(final ClassificationRegistry classificationRegistry, final ModuleCoordinates coordinates) {
            stubModule(classificationRegistry, coordinates, mock(Classification.class));
        }

        private void stubModule(final ClassificationRegistry classificationRegistry, final ModuleCoordinates coordinates, final Classification classification) {
            final var module = mock(ClassifiedModule.class);
            when(module.getModuleCoordinates()).thenReturn(coordinates);
            when(module.getClassification()).thenReturn(classification);
            when(classificationRegistry.getClassifiedModule(coordinates)).thenReturn(module);
        }
    }
}
