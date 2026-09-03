package io.github.march_plugin.core.enforcement.project;

import io.github.march_plugin.core.config.classification.exception.ModuleNotClassifiedException;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedComponent;
import io.github.march_plugin.core.config.classification.model.ClassifiedConcreteModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModule;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.enforcement.project.exceptions.ClassifiedModuleDoesNotExistException;
import io.github.march_plugin.core.enforcement.project.exceptions.ClassifiedPackageNotFoundException;
import io.github.march_plugin.core.enforcement.project.exceptions.CleanRootPathViolationException;
import io.github.march_plugin.core.enforcement.project.exceptions.JavaRootPackageNotFoundException;
import io.github.march_plugin.core.enforcement.project.exceptions.ModuleContainsForbiddenCodeException;
import io.github.march_plugin.core.enforcement.project.exceptions.PackageNotClassifiedException;
import io.github.march_plugin.core.enforcement.project.exceptions.RootPackageNotFoundException;
import io.github.march_plugin.core.project.ProjectModuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.github.march_plugin.core.config.testutil.MockUtil.mockModuleModularity;
import static io.github.march_plugin.core.config.testutil.MockUtil.mockPackageModularity;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectComponentEnforcerTest {

    @TempDir
    private Path tempDir;

    private final ProjectComponentEnforcer enforcer = new ProjectComponentEnforcer();
    private final ClassifiedConcreteModule projectRoot = new ClassifiedConcreteModule.Builder(new ModuleCoordinates("io.example", "root"), null).buildAsRoot();

    private int dimensionCounter;

    private Dimension.Partition freshPartition() {
        final var name = "dim" + ++dimensionCounter;
        final var builder = new Dimension.Builder(name);
        final var partition = builder.addPartition(name + "-a");
        builder.addPartition(name + "-b");
        builder.build();
        return partition;
    }

    private ClassifiedConcreteModule module(final ModuleCoordinates coordinates, final PackageHierarchy rootPackage) {
        return new ClassifiedConcreteModule.Builder(coordinates, freshPartition())
                .setRootPackage(rootPackage)
                .buildAsChild(projectRoot, mockModuleModularity());
    }

    private ClassifiedPackage classifiedPackage(final ClassifiedComponent parent, final PackageHierarchy hierarchy) {
        return new ClassifiedPackage.Builder(parent.getModuleCoordinates(), freshPartition(), hierarchy)
                .buildAsChild(parent, mockPackageModularity());
    }

    private ClassifiedPackage optionalPackage(final ClassifiedComponent parent, final PackageHierarchy hierarchy) {
        return new ClassifiedPackage.Builder(parent.getModuleCoordinates(), freshPartition(), hierarchy)
                .setOptional()
                .buildAsChild(parent, mockPackageModularity());
    }

    private void mkdirs(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createFile(final Path path) {
        try {
            Files.createFile(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nested
    class EnforcePackageStructureForClassifiedModule {

        @Test
        void shouldDelegateToConcreteModuleOverload() {
            final var coordinates = new ModuleCoordinates("io.example", "app-module");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            mkdirs(tempDir.resolve("src").resolve("main").resolve("java").resolve("app"));
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(tempDir, null, List.of(), List.of());

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, moduleInfo))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldPropagateViolationsFromConcreteModuleOverload() {
            final var coordinates = new ModuleCoordinates("io.example", "app-module");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(tempDir, null, List.of(), List.of());

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, moduleInfo))
                    .isInstanceOf(JavaRootPackageNotFoundException.class);
        }
    }

    @Nested
    class ModuleWithoutRootPackage {

        @Test
        void shouldPassWhenNoSrcDirectoryExists() {
            final var coordinates = new ModuleCoordinates("io.example", "aggregator");
            final var classifiedModule = module(coordinates, null);

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenSrcDirectoryExists() {
            final var coordinates = new ModuleCoordinates("io.example", "aggregator");
            final var classifiedModule = module(coordinates, null);
            mkdirs(tempDir.resolve("src"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(ModuleContainsForbiddenCodeException.class)
                    .hasMessageContaining(coordinates.toString());
        }

        @Test
        void shouldThrowWhenSrcExistsAsPlainFile() {
            final var coordinates = new ModuleCoordinates("io.example", "aggregator");
            final var classifiedModule = module(coordinates, null);
            createFile(tempDir.resolve("src"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(ModuleContainsForbiddenCodeException.class);
        }
    }

    @Nested
    class RootPackagePathValidation {

        @Test
        void shouldThrowWhenJavaRootMissing() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(JavaRootPackageNotFoundException.class)
                    .hasMessageContaining(coordinates.toString());
        }

        @Test
        void shouldThrowWhenJavaRootContainsUnexpectedDirectory() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            final var javaRoot = tempDir.resolve("src").resolve("main").resolve("java");
            mkdirs(javaRoot.resolve("app"));
            mkdirs(javaRoot.resolve("unexpected"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(CleanRootPathViolationException.class)
                    .hasMessageContaining("unexpected");
        }

        @Test
        void shouldThrowWhenJavaRootContainsStrayFile() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            final var javaRoot = tempDir.resolve("src").resolve("main").resolve("java");
            mkdirs(javaRoot.resolve("app"));
            createFile(javaRoot.resolve("Stray.java"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(CleanRootPathViolationException.class)
                    .hasMessageContaining("Stray.java");
        }

        @Test
        void shouldThrowWhenExpectedFirstSegmentMissing() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("com", "example")));
            mkdirs(tempDir.resolve("src").resolve("main").resolve("java"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(RootPackageNotFoundException.class);
        }

        @Test
        void shouldThrowWhenSecondSegmentHasStrayFile() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("com", "example")));
            final var javaRoot = tempDir.resolve("src").resolve("main").resolve("java");
            mkdirs(javaRoot.resolve("com").resolve("example"));
            createFile(javaRoot.resolve("com").resolve("Stray.java"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(CleanRootPathViolationException.class)
                    .hasMessageContaining("Stray.java");
        }

        @Test
        void shouldThrowWhenSecondSegmentHasUnexpectedDirectory() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("com", "example")));
            final var javaRoot = tempDir.resolve("src").resolve("main").resolve("java");
            mkdirs(javaRoot.resolve("com").resolve("example"));
            mkdirs(javaRoot.resolve("com").resolve("other"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(CleanRootPathViolationException.class)
                    .hasMessageContaining("other");
        }

        @Test
        void shouldPassWhenMultiSegmentRootPackageExistsCleanly() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("com", "example")));
            mkdirs(tempDir.resolve("src").resolve("main").resolve("java").resolve("com").resolve("example"));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class PackageLayerValidation {

        private Path rootPackageDir;

        @BeforeEach
        void setUp() {
            rootPackageDir = tempDir.resolve("src").resolve("main").resolve("java").resolve("app");
            mkdirs(rootPackageDir);
        }

        @Test
        void shouldPassWithNoDeclaredOrPresentPackages() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldPassWhenMandatoryPackageExists() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            mkdirs(rootPackageDir.resolve("service"));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenMandatoryPackageMissing() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(ClassifiedPackageNotFoundException.class)
                    .hasMessageContaining("app.service");
        }

        @Test
        void shouldPassWhenOptionalPackageMissing() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            optionalPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenUndeclaredPackageDirectoryExists() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            mkdirs(rootPackageDir.resolve("undeclared"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(PackageNotClassifiedException.class)
                    .hasMessageContaining("undeclared")
                    .hasMessageContaining(coordinates.toString());
        }

        @Test
        void shouldPassWhenMultipleMandatoryPackagesAllExist() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "web")));
            mkdirs(rootPackageDir.resolve("service"));
            mkdirs(rootPackageDir.resolve("web"));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldValidateNestedMandatoryPackagesRecursively() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            final var service = classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            classifiedPackage(service, new PackageHierarchy(List.of("app", "service", "impl")));
            mkdirs(rootPackageDir.resolve("service").resolve("impl"));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenNestedMandatoryPackageMissing() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            final var service = classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            classifiedPackage(service, new PackageHierarchy(List.of("app", "service", "impl")));
            mkdirs(rootPackageDir.resolve("service"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(ClassifiedPackageNotFoundException.class)
                    .hasMessageContaining("app.service.impl");
        }

        @Test
        void shouldThrowWhenNestedUndeclaredPackageExists() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            final var service = classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            classifiedPackage(service, new PackageHierarchy(List.of("app", "service", "impl")));
            mkdirs(rootPackageDir.resolve("service").resolve("impl"));
            mkdirs(rootPackageDir.resolve("service").resolve("extra"));

            assertThatThrownBy(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .isInstanceOf(PackageNotClassifiedException.class)
                    .hasMessageContaining("extra");
        }

        @Test
        void shouldNotDescendIntoLeafPackageContents() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            mkdirs(rootPackageDir.resolve("service").resolve("unvalidated-subpackage"));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldSkipVirtualModuleChildrenWhenValidatingPackageLayer() {
            // A concrete module's children can be virtual modules instead of packages (classifying an external
            // dependency it depends on) -- those don't correspond to any real directory and must not be cast to
            // ClassifiedPackage, nor checked against the filesystem the way declared packages are.
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            new ClassifiedVirtualModule.Builder(new ModuleCoordinates("io.external", "lib"), freshPartition())
                    .buildAsChild(classifiedModule, mockModuleModularity());

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldIgnoreFilesWithinAndBelowRootPackage() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));
            classifiedPackage(classifiedModule, new PackageHierarchy(List.of("app", "service")));
            mkdirs(rootPackageDir.resolve("service"));
            createFile(rootPackageDir.resolve("App.java"));
            createFile(rootPackageDir.resolve("service").resolve("MyService.java"));

            assertThatCode(() -> enforcer.enforcePackageStructure(classifiedModule, coordinates, tempDir))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ValidateComponentExistence {

        @Test
        void shouldPassWhenBothRegistriesAreEmpty() {
            final var classificationRegistry = new ClassificationRegistry.Builder().build();
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of());

            assertThatCode(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldPassWhenExistingModuleMatchesClassification() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, null);

            final var registryBuilder = new ClassificationRegistry.Builder();
            registryBuilder.addModuleClassification(classifiedModule);
            final var classificationRegistry = registryBuilder.build();

            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(tempDir, null, List.of(), List.of());
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of(coordinates, moduleInfo));

            assertThatCode(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenClassifiedModuleDoesNotExistInProject() {
            final var coordinates = new ModuleCoordinates("io.example", "missing");
            final var classifiedModule = module(coordinates, null);

            final var registryBuilder = new ClassificationRegistry.Builder();
            registryBuilder.addModuleClassification(classifiedModule);
            final var classificationRegistry = registryBuilder.build();
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of());

            assertThatThrownBy(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .isInstanceOf(ClassifiedModuleDoesNotExistException.class)
                    .hasMessageContaining(coordinates.toString());
        }

        @Test
        void shouldThrowWhenExistingModuleIsNotClassified() {
            final var coordinates = new ModuleCoordinates("io.example", "unclassified");
            final var classificationRegistry = new ClassificationRegistry.Builder().build();

            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(tempDir, null, List.of(), List.of());
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of(coordinates, moduleInfo));

            assertThatThrownBy(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .isInstanceOf(ModuleNotClassifiedException.class);
        }

        @Test
        void shouldNotRequireVirtualModulesToExistInProject() {
            final var virtualModule = new ClassifiedVirtualModule.Builder(new ModuleCoordinates("io.example", "virtual"), freshPartition())
                    .buildAsChild(projectRoot, mockModuleModularity());

            final var registryBuilder = new ClassificationRegistry.Builder();
            registryBuilder.addModuleClassification(virtualModule);
            final var classificationRegistry = registryBuilder.build();
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of());

            assertThatCode(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldValidateEveryExistingModulesPackageStructure() {
            final var coordinates = new ModuleCoordinates("io.example", "app");
            final var classifiedModule = module(coordinates, new PackageHierarchy(List.of("app")));

            final var registryBuilder = new ClassificationRegistry.Builder();
            registryBuilder.addModuleClassification(classifiedModule);
            final var classificationRegistry = registryBuilder.build();

            // src/main/java is intentionally missing to trigger a structure violation
            final var moduleInfo = new ProjectModuleRegistry.ProjectModuleInfo(tempDir, null, List.of(), List.of());
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of(coordinates, moduleInfo));

            assertThatThrownBy(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .isInstanceOf(JavaRootPackageNotFoundException.class);
        }

        @Test
        void shouldPassForMultipleValidModules() {
            final var coordinatesA = new ModuleCoordinates("io.example", "module-a");
            final var coordinatesB = new ModuleCoordinates("io.example", "module-b");
            final var moduleA = module(coordinatesA, null);
            final var moduleB = module(coordinatesB, null);

            final var registryBuilder = new ClassificationRegistry.Builder();
            registryBuilder.addModuleClassification(moduleA);
            registryBuilder.addModuleClassification(moduleB);
            final var classificationRegistry = registryBuilder.build();

            final var baseDirA = tempDir.resolve("module-a");
            final var baseDirB = tempDir.resolve("module-b");
            mkdirs(baseDirA);
            mkdirs(baseDirB);

            final var moduleInfoA = new ProjectModuleRegistry.ProjectModuleInfo(baseDirA, null, List.of(), List.of());
            final var moduleInfoB = new ProjectModuleRegistry.ProjectModuleInfo(baseDirB, null, List.of(), List.of());
            final var projectModuleRegistry = new ProjectModuleRegistry(Map.of(coordinatesA, moduleInfoA, coordinatesB, moduleInfoB));

            assertThatCode(() -> enforcer.validateComponentExistence(projectModuleRegistry, classificationRegistry))
                    .doesNotThrowAnyException();
        }
    }
}
