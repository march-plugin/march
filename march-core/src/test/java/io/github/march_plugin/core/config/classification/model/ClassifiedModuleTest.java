package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.testutil.TestUtil;
import org.junit.jupiter.api.Test;

import static io.github.march_plugin.core.config.testutil.MockUtil.mockModuleModularity;
import static io.github.march_plugin.core.config.testutil.MockUtil.mockPackageModularity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClassifiedModuleTest {

    private final TestUtil testUtil = new TestUtil();

    @Test
    void shouldBeLeafWhenItHasNoChildren() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();

        assertThat(root.isLeafModule()).isTrue();
    }

    @Test
    void shouldBeLeafWhenItOnlyHasPackageChildren() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        new ClassifiedPackage.Builder(mock(ModuleCoordinates.class), testUtil.presentationPartition, mock(PackageHierarchy.class))
                .buildAsChild(root, mockPackageModularity());

        assertThat(root.isLeafModule()).isTrue();
    }

    @Test
    void shouldNotBeLeafWhenItHasModuleChildren() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), testUtil.presentationPartition)
                .buildAsChild(root, mockModuleModularity());

        assertThat(root.isLeafModule()).isFalse();
    }
}
