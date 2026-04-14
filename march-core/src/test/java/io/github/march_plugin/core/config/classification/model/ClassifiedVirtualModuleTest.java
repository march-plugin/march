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
        final var virtual = new ClassifiedVirtualModule.Builder(mock(ModuleCoordinates.class))
                .buildAsChild(new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class)).buildAsRoot(), testUtil.articlePartition, mockModuleModularity());

        final var pkgBuilder = new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), mock(PackageHierarchy.class));

        final var packageModularity = mock(PackageModularity.class);
        when(packageModularity.getConvention()).thenReturn(mock(PackageConvention.class));

        assertThatThrownBy(() -> pkgBuilder.buildAsChild(virtual, mock(Dimension.Partition.class), packageModularity))
                .isInstanceOf(ClassifiedVirtualModuleChildrenException.class);
    }

    @Test
    void shouldThrowWhenAddingAnyChildToVirtualReference() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class)).buildAsRoot();
        final var ref = new ClassifiedVirtualModuleReference.Builder(mock(ModuleCoordinates.class), mock(ModuleCoordinates.class))
                .buildAsChild(root, testUtil.articlePartition, mockModuleModularity());

        final var childBuilder = new ClassifiedVirtualModule.Builder(mock(ModuleCoordinates.class));

        assertThatThrownBy(() -> childBuilder.buildAsChild(ref, mock(Dimension.Partition.class), mockModuleModularity()))
                .isInstanceOf(ClassifiedVirtualModuleReferenceChildrenException.class);
    }

    @Test
    void shouldExposeExternalCoordinates() {
        final var extCoords = new ModuleCoordinates("external", "artifact");
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class)).buildAsRoot();
        final var ref = new ClassifiedVirtualModuleReference.Builder(mock(ModuleCoordinates.class), extCoords)
                .buildAsChild(root, testUtil.articlePartition, mockModuleModularity());

        assertThat(ref.getExternalCoordinates()).isEqualTo(extCoords);
    }
}