package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedVirtualModuleChildrenException;
import io.github.march_plugin.core.config.classification.exception.ClassifiedVirtualModuleReferenceChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import io.github.march_plugin.core.config.testutil.TestUtil;
import org.junit.jupiter.api.Test;

import static io.github.march_plugin.core.config.testutil.MockUtil.mockModuleModularity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassifiedVirtualModuleTest {

    private final TestUtil testUtil = new TestUtil();

    @Test
    void shouldThrowWhenAddingPackageToVirtualModule() {
        final var virtual = new ClassifiedVirtualModule.Builder(mock(ModuleCoordinates.class), testUtil.articlePartition)
                .buildAsChild(new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot(), mockModuleModularity());

        final var pkgBuilder = new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), mock(Dimension.Partition.class), mock(PackageHierarchy.class));

        final var packageModularity = mock(PackageModularity.class);
        when(packageModularity.getConvention()).thenReturn(mock(PackageConvention.class));

        assertThatThrownBy(() -> pkgBuilder.buildAsChild(virtual, packageModularity))
                .isInstanceOf(ClassifiedVirtualModuleChildrenException.class);
    }

    @Test
    void shouldThrowWhenAddingAnyChildToVirtualReference() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var ref = new ClassifiedVirtualModuleReference.Builder(mock(ModuleCoordinates.class), testUtil.articlePartition, mock(ModuleCoordinates.class))
                .buildAsChild(root, mockModuleModularity());

        final var childBuilder = new ClassifiedVirtualModule.Builder(mock(ModuleCoordinates.class), mock(Dimension.Partition.class));

        assertThatThrownBy(() -> childBuilder.buildAsChild(ref, mockModuleModularity()))
                .isInstanceOf(ClassifiedVirtualModuleReferenceChildrenException.class);
    }

    @Test
    void shouldExposeExternalCoordinates() {
        final var extCoords = new ModuleCoordinates("external", "artifact");
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var ref = new ClassifiedVirtualModuleReference.Builder(mock(ModuleCoordinates.class), testUtil.articlePartition, extCoords)
                .buildAsChild(root, mockModuleModularity());

        assertThat(ref.getExternalCoordinates()).isEqualTo(extCoords);
    }
}