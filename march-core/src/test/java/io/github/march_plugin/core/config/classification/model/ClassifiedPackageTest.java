package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedPackageChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.testutil.TestUtil;
import org.junit.jupiter.api.Test;

import static io.github.march_plugin.core.config.testutil.MockUtil.mockModuleModularity;
import static io.github.march_plugin.core.config.testutil.MockUtil.mockPackageModularity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClassifiedPackageTest {

    private final TestUtil testUtil = new TestUtil();

    @Test
    void shouldPersistOptionalFlag() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var pkg = new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), testUtil.articlePartition, mock(PackageHierarchy.class))
                .setOptional()
                .buildAsChild(root, mockPackageModularity());

        assertThat(pkg.isOptional()).isTrue();
    }

    @Test
    void shouldThrowWhenAddingModuleToPackage() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var pkg = new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), testUtil.articlePartition, mock(PackageHierarchy.class))
                .buildAsChild(root, mockPackageModularity());

        final var moduleBuilder = new ClassifiedVirtualModule.Builder(mock(ModuleCoordinates.class), mock(Dimension.Partition.class));

        assertThatThrownBy(() -> moduleBuilder.buildAsChild(pkg, mockModuleModularity()))
                .isInstanceOf(ClassifiedPackageChildrenException.class);
    }

    @Test
    void shouldIdentifyLeafStatusInPackageClassification() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var pkg = new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), testUtil.articlePartition, mock(PackageHierarchy.class))
                .buildAsChild(root, mockPackageModularity());

        final var classification = pkg.getClassifiedPackage();
        assertThat(classification.isClassificationLeaf()).isTrue();
    }

    @Test
    void shouldReturnCorrectPackageCoordinates() {
        final var moduleCoords = mock(ModuleCoordinates.class);
        final var packageHierarchy = mock(PackageHierarchy.class);
        final var root = new ClassifiedConcreteModule.Builder(moduleCoords, null).buildAsRoot();

        final var classifiedPackage = new ClassifiedPackage.Builder(moduleCoords, testUtil.articlePartition, packageHierarchy)
                .buildAsChild(root, mockPackageModularity());

        final var result = classifiedPackage.getPackageCoordinates();

        assertThat(result).isNotNull();
        assertThat(result.moduleCoordinates()).isEqualTo(moduleCoords);
        assertThat(result.packageHierarchy()).isEqualTo(packageHierarchy);
    }
}