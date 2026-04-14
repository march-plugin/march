package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedConcreteModuleChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import org.junit.jupiter.api.Test;

import static io.github.march_plugin.core.config.testutil.MockUtil.mockModuleModularity;
import static io.github.march_plugin.core.config.testutil.MockUtil.mockPackageModularity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClassifiedConcreteModuleTest {

    @Test
    void shouldBuildRootModuleWithEmptyClassification() {
        final var coords = new ModuleCoordinates("g", "a");
        final var root = new ClassifiedConcreteModule.Builder(coords).buildAsRoot();

        assertThat(root.getPartition()).isNull();
        assertThat(root.getRootPackage()).isNull();
        assertThat(root.getClassification().getPartitions()).isEmpty();
    }

    @Test
    void shouldThrowWhenMixingPackageAndModuleChildren() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class)).buildAsRoot();

        final var pkgBuilder = new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), mock(PackageHierarchy.class));
        pkgBuilder.buildAsChild(root, mock(Dimension.Partition.class), mockPackageModularity());

        final var moduleBuilder = new ClassifiedVirtualModule.Builder(mock(ModuleCoordinates.class));

        assertThatThrownBy(() -> moduleBuilder.buildAsChild(root, mock(Dimension.Partition.class), mockModuleModularity()))
                .isInstanceOf(ClassifiedConcreteModuleChildrenException.class);
    }
}