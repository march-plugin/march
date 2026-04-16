package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.DuplicateClassificationException;
import io.github.march_plugin.core.config.classification.exception.DuplicateModuleException;
import io.github.march_plugin.core.config.classification.exception.DuplicatePackageException;
import io.github.march_plugin.core.config.classification.exception.ModuleNotClassifiedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ClassificationRegistryTest {

    private ClassificationRegistry.Builder builder;

    @BeforeEach
    void setUp() {
        builder = new ClassificationRegistry.Builder();
    }

    @Test
    void testAddModuleAndPackageSuccess() {
        final var moduleCoordinates = Mockito.mock(ModuleCoordinates.class);
        final var classifiedModule = Mockito.mock(ClassifiedModule.class);
        final var classification = Mockito.mock(Classification.class);
        when(classifiedModule.getModuleCoordinates()).thenReturn(moduleCoordinates);
        when(classifiedModule.getClassification()).thenReturn(classification);

        final var packageCoordinates = Mockito.mock(PackageCoordinates.class);
        final var classifiedPackage = Mockito.mock(ClassifiedPackage.class);
        final var pClassification = Mockito.mock(Classification.class);
        when(classifiedPackage.getPackageCoordinates()).thenReturn(packageCoordinates);
        when(classifiedPackage.getClassification()).thenReturn(pClassification);

        builder.addModuleClassification(classifiedModule);
        builder.addPackageClassification(classifiedPackage);
        final var registry = builder.build();

        assertThat(registry.getClassifiedModule(moduleCoordinates)).isEqualTo(classifiedModule);
        assertThat(registry.getAllClassifiedPackages())
                .hasSize(1)
                .contains(classifiedPackage);
    }

    @Test
    void testDuplicateModuleInBuilderThrowsException() {
        final var moduleCoordinates = Mockito.mock(ModuleCoordinates.class);
        final var classifiedModule = Mockito.mock(ClassifiedModule.class);
        when(classifiedModule.getModuleCoordinates()).thenReturn(moduleCoordinates);

        builder.addModuleClassification(classifiedModule);

        assertThrows(DuplicateModuleException.class, () -> builder.addModuleClassification(classifiedModule));
    }

    @Test
    void testDuplicatePackageInBuilderThrowsException() {
        final var packageCoordinates = Mockito.mock(PackageCoordinates.class);
        final var classifiedPackage = Mockito.mock(ClassifiedPackage.class);
        when(classifiedPackage.getPackageCoordinates()).thenReturn(packageCoordinates);

        builder.addPackageClassification(classifiedPackage);

        assertThrows(DuplicatePackageException.class, () -> builder.addPackageClassification(classifiedPackage));
    }

    @Test
    void testDuplicateClassificationCrossModuleThrowsException() {
        final var sharedClassification = Mockito.mock(Classification.class);

        final var moduleCoordinates1 = Mockito.mock(ModuleCoordinates.class);
        final var classifiedModule1 = Mockito.mock(ClassifiedModule.class);
        when(classifiedModule1.getModuleCoordinates()).thenReturn(moduleCoordinates1);
        when(classifiedModule1.getClassification()).thenReturn(sharedClassification);

        final var moduleCoordinates2 = Mockito.mock(ModuleCoordinates.class);
        final var classifiedModule2 = Mockito.mock(ClassifiedModule.class);
        when(classifiedModule2.getModuleCoordinates()).thenReturn(moduleCoordinates2);
        when(classifiedModule2.getClassification()).thenReturn(sharedClassification);

        builder.addModuleClassification(classifiedModule1);
        assertThrows(DuplicateClassificationException.class, () -> builder.addModuleClassification(classifiedModule2));
    }

    @Test
    void testDuplicateClassificationCrossPackageThrowsException() {
        final var sharedClassification = Mockito.mock(Classification.class);

        final var moduleCoordinates = Mockito.mock(ModuleCoordinates.class);
        final var classifiedModule = Mockito.mock(ClassifiedModule.class);
        when(classifiedModule.getModuleCoordinates()).thenReturn(moduleCoordinates);
        when(classifiedModule.getClassification()).thenReturn(sharedClassification);

        final var ppackageCoordinates = Mockito.mock(PackageCoordinates.class);
        final var classifiedPackage = Mockito.mock(ClassifiedPackage.class);
        when(classifiedPackage.getPackageCoordinates()).thenReturn(ppackageCoordinates);
        when(classifiedPackage.getClassification()).thenReturn(sharedClassification);

        builder.addModuleClassification(classifiedModule);
        assertThrows(DuplicateClassificationException.class, () -> builder.addPackageClassification(classifiedPackage));
    }

    @Test
    void testGetAllModulesOfTypeFilter() {
        final var moduleCoordinates = Mockito.mock(ModuleCoordinates.class);
        final var classifiedModule = Mockito.mock(ClassifiedModule.class);
        when(classifiedModule.getModuleCoordinates()).thenReturn(moduleCoordinates);
        when(classifiedModule.getClassification()).thenReturn(Mockito.mock(Classification.class));

        builder.addModuleClassification(classifiedModule);
        final var registry = builder.build();

        final var results = registry.getAllModulesOfType(classifiedModule.getClass());

        assertThat(results)
                .isNotEmpty()
                .contains(classifiedModule);
    }

    @Test
    void testGetClassifiedModuleVirtualReference() {
        final var externalmoduleCoordinates = Mockito.mock(ModuleCoordinates.class);
        final var internalmoduleCoordinates = Mockito.mock(ModuleCoordinates.class);

        final var virtualMod = Mockito.mock(ClassifiedVirtualModuleReference.class);
        when(virtualMod.getModuleCoordinates()).thenReturn(internalmoduleCoordinates);
        when(virtualMod.getExternalCoordinates()).thenReturn(externalmoduleCoordinates);
        when(virtualMod.getClassification()).thenReturn(Mockito.mock(Classification.class));

        builder.addModuleClassification(virtualMod);
        final var registry = builder.build();

        final var result = registry.getClassifiedModule(externalmoduleCoordinates);
        assertThat(virtualMod).isEqualTo(result);
    }

    @Test
    void testGetClassifiedModuleNotFoundThrowsException() {
        final var registry = builder.build();
        final var missing = Mockito.mock(ModuleCoordinates.class);

        assertThrows(ModuleNotClassifiedException.class, () -> registry.getClassifiedModule(missing));
    }
}