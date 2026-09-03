package io.github.march_plugin.core.enforcement.project;

import io.github.march_plugin.core.config.classification.exception.ModuleNotClassifiedException;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedComponent;
import io.github.march_plugin.core.config.classification.model.ClassifiedConcreteModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.enforcement.project.exceptions.ClassifiedModuleDoesNotExistException;
import io.github.march_plugin.core.enforcement.project.exceptions.ClassifiedPackageNotFoundException;
import io.github.march_plugin.core.enforcement.project.exceptions.CleanRootPathViolationException;
import io.github.march_plugin.core.enforcement.project.exceptions.JavaRootPackageNotFoundException;
import io.github.march_plugin.core.enforcement.project.exceptions.ModuleContainsForbiddenCodeException;
import io.github.march_plugin.core.enforcement.project.exceptions.PackageNotClassifiedException;
import io.github.march_plugin.core.enforcement.project.exceptions.ProjectAnalysisIOException;
import io.github.march_plugin.core.enforcement.project.exceptions.RootPackageNotFoundException;
import io.github.march_plugin.core.project.ProjectModuleRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enforces that the physical structure of the project matches the modules and packages classified in march config.
 */
public class ProjectComponentEnforcer {

    /**
     * Validates the existence of all configured components.
     *
     * This enforces, in both directions, that every module found in the project is classified and every
     * classified module actually exists. Additionally, the package structure of every existing module is
     * validated against its classified packages.
     *
     * @param projectModuleRegistry the registry containing all modules found in the project
     * @param classificationRegistry the registry containing all classifications configured in march config
     * @throws ModuleNotClassifiedException if an existing module is not classified
     * @throws ClassifiedModuleDoesNotExistException if a classified module does not exist in the project
     */
    public void validateComponentExistence(
            final ProjectModuleRegistry projectModuleRegistry,
            final ClassificationRegistry classificationRegistry) {

        final var existingModuleCoordinates = projectModuleRegistry.getAllProjectModules();
        final var notFoundClassifiedModules = classificationRegistry.getAllModulesOfType(ClassifiedConcreteModule.class)
                .stream()
                .map(ClassifiedComponent::getModuleCoordinates)
                .collect(Collectors.toSet());

        for (final var existingModule : existingModuleCoordinates.entrySet()) {
            final var coordinates = existingModule.getKey();
            final var classifiedModule = classificationRegistry.getClassifiedModule(coordinates);
            enforcePackageStructure(classifiedModule, existingModule.getValue());

            notFoundClassifiedModules.remove(coordinates);
        }

        // Ensure all classified modules exist
        if (!notFoundClassifiedModules.isEmpty()) {
            throw new ClassifiedModuleDoesNotExistException(notFoundClassifiedModules.iterator().next().toString());
        }
    }

    /**
     * Enforces the package structure of a module if it is a {@link ClassifiedConcreteModule}. Modules that are not
     * concrete, e.g. virtual modules, do not correspond to a physical module of the project and are skipped.
     *
     * @param classifiedModule the classification of the module
     * @param projectModuleInfo the physical information about the module found in the project
     */
    public void enforcePackageStructure(final ClassifiedModule classifiedModule, final ProjectModuleRegistry.ProjectModuleInfo projectModuleInfo) {
        if (classifiedModule instanceof ClassifiedConcreteModule classifiedConcreteModule) {
            enforcePackageStructure(classifiedConcreteModule, classifiedModule.getModuleCoordinates(), projectModuleInfo.baseDir());
        }
    }

    /**
     * Enforces the package structure configured for a module.
     *
     * If no root package is configured, the module must not contain any source code. Otherwise, the path
     * leading to the root package must be clean, and the packages beneath the root package must match the
     * classified packages of the module.
     *
     * @param classifiedConcreteModule the classification of the module
     * @param moduleCoordinates the coordinates of the module
     * @param baseDir the base directory of the module on disk
     * @throws ModuleContainsForbiddenCodeException if the module has no root package configured but contains code
     * @throws JavaRootPackageNotFoundException if a root package is configured but {@code src/main/java} is missing
     * @throws CleanRootPathViolationException if the path leading to the root package is not clean
     * @throws RootPackageNotFoundException if the configured root package does not exist
     * @throws PackageNotClassifiedException if an existing package is not classified
     * @throws ClassifiedPackageNotFoundException if a mandatory classified package does not exist
     */
    public void enforcePackageStructure(final ClassifiedConcreteModule classifiedConcreteModule, final ModuleCoordinates moduleCoordinates, final Path baseDir) {
        final var configuredRootPackage = classifiedConcreteModule.getRootPackage();
        if (configuredRootPackage == null || configuredRootPackage.depth() == 0) {
            validateModuleDoesNotContainCode(baseDir, moduleCoordinates);
            return;
        }

        final var javaRoot = validateJavaDirExists(baseDir, moduleCoordinates);
        final var rootPackageDir = validateOnlyExpectedPathToRootPackage(configuredRootPackage, javaRoot, moduleCoordinates);

        enforcePackageLayer(configuredRootPackage, moduleCoordinates, toClassifiedPackages(classifiedConcreteModule.getChildren()), rootPackageDir);
    }

    /**
     * Recursively enforces that a package layer matches its classified packages: every existing subpackage must be
     * classified, and every mandatory classified package must exist on disk.
     *
     * @param packageHierarchy the hierarchy of the currently enforced layer
     * @param moduleCoordinates the coordinates of the module the layer belongs to
     * @param classifiedPackages the packages classified as children of the layer
     * @param currentPackage the directory of the currently enforced layer
     */
    private void enforcePackageLayer(final PackageHierarchy packageHierarchy, final ModuleCoordinates moduleCoordinates, final List<ClassifiedPackage> classifiedPackages, final Path currentPackage) {
        final var subDirs = getSubDirs(currentPackage);
        final var foundClassifiedPackages = new HashSet<ClassifiedPackage>();

        for (final var subDir : subDirs) {
            final var subPackageName = subDir.getFileName().toString();
            final var childPackageHierarchy = PackageHierarchy.buildChild(packageHierarchy, subPackageName);

            final var childPackage = classifiedPackages.stream()
                    .filter(c -> c.getPackageHierarchy().equals(childPackageHierarchy))
                    .findFirst()
                    .orElseThrow(() -> new PackageNotClassifiedException(subPackageName, moduleCoordinates.toString()));

            foundClassifiedPackages.add(childPackage);

            if (childPackage.getChildren().isEmpty()) {
                continue;
            }

            enforcePackageLayer(childPackageHierarchy, moduleCoordinates, toClassifiedPackages(childPackage.getChildren()), subDir);
        }

        // Ensure all mandatory packages of this layer are present
        for (final var classifiedPackage : classifiedPackages) {
            if (!classifiedPackage.isOptional() && !foundClassifiedPackages.contains(classifiedPackage)) {
                throw new ClassifiedPackageNotFoundException(classifiedPackage.getPackageHierarchy().toString());
            }
        }
    }

    private List<ClassifiedPackage> toClassifiedPackages(final List<ClassifiedComponent> children) {
        return children.stream()
                .filter(ClassifiedPackage.class::isInstance)
                .map(ClassifiedPackage.class::cast)
                .toList();
    }

    private void validateModuleDoesNotContainCode(final Path baseDir, final ModuleCoordinates moduleCoordinates) {
        if (Files.exists(baseDir.resolve("src"))) {
            throw new ModuleContainsForbiddenCodeException(moduleCoordinates.toString());
        }
    }

    /**
     * Walks down from the java root to the configured root package, validating along the way that the path leading
     * to it is clean.
     *
     * @param rootPackage the configured root package of the module
     * @param javaRoot the java root ({@code src/main/java}) of the module
     * @param moduleCoordinates the coordinates of the module
     * @return the directory of the root package
     */
    private Path validateOnlyExpectedPathToRootPackage(final PackageHierarchy rootPackage, final Path javaRoot, final ModuleCoordinates moduleCoordinates) {
        var currentPath = javaRoot;

        for (var i = 0; i < rootPackage.depth(); i++) {
            final var expectedChildPackage = rootPackage.get(i);
            currentPath = validateOnlyExpectedChildPackageInPackage(rootPackage, currentPath, expectedChildPackage, moduleCoordinates);
        }
        return currentPath;
    }

    /**
     * Validates that a package on the path to the root package contains no files and no package other than the
     * expected child package.
     *
     * @param rootPackage the configured root package of the module
     * @param path the package to validate
     * @param expectedChildPackage the only package allowed to exist in {@code path}
     * @param moduleCoordinates the coordinates of the module
     * @return the directory of the expected child package
     */
    private Path validateOnlyExpectedChildPackageInPackage(final PackageHierarchy rootPackage, final Path path, final String expectedChildPackage, final ModuleCoordinates moduleCoordinates) {
        validateNoFilesInPackage(path, rootPackage, moduleCoordinates);

        final var subDirs = getSubDirs(path);

        final var invalidSubDir = subDirs.stream().filter(s -> !s.getFileName().toString().equals(expectedChildPackage)).findFirst();
        if (invalidSubDir.isPresent()) {
            throw new CleanRootPathViolationException(moduleCoordinates.toString(), rootPackage.toString(), invalidSubDir.get().toString());
        }

        return subDirs.stream()
                .filter(p -> p.getFileName().toString().equals(expectedChildPackage))
                .findFirst()
                .orElseThrow(() -> new RootPackageNotFoundException(moduleCoordinates.toString(), rootPackage.toString()));
    }

    private List<Path> getSubDirs(final Path path) {
        try (Stream<Path> list = Files.list(path)) {
            return list.filter(Files::isDirectory).toList();
        } catch (final IOException e) {
            throw new ProjectAnalysisIOException(path.toString());
        }
    }

    private void validateNoFilesInPackage(final Path path, final PackageHierarchy rootPackage, final ModuleCoordinates moduleCoordinates) {
        try (Stream<Path> list = Files.list(path)) {
            final var violatingFile = list.filter(Files::isRegularFile).findFirst();
            if (violatingFile.isPresent()) {
                throw new CleanRootPathViolationException(moduleCoordinates.toString(), rootPackage.toString(), violatingFile.get().toString());
            }
        } catch (final IOException e) {
            throw new ProjectAnalysisIOException(path.toString());
        }
    }

    private Path validateJavaDirExists(final Path baseDir, final ModuleCoordinates moduleCoordinates) {
        final var javaRoot = baseDir.resolve("src").resolve("main").resolve("java");

        if (!Files.exists(javaRoot)) {
            throw new JavaRootPackageNotFoundException(moduleCoordinates.toString());
        }
        return javaRoot;
    }
}
