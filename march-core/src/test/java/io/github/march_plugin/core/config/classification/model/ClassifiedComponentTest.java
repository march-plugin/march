package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ComponentPartitionNotDefinedException;
import io.github.march_plugin.core.config.classification.exception.DuplicatePartitionClassificationException;
import io.github.march_plugin.core.config.classification.exception.PartitionNotAllowedException;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.testutil.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.github.march_plugin.core.config.testutil.MockUtil.mockModuleModularity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassifiedComponentTest {

    private final TestUtil testUtil = new TestUtil();

    @Test
    void shouldThrowWhenBuildingAsChildWithoutPartition() {
        final var coords = new ModuleCoordinates("io.test", "a");
        final var builder = new ClassifiedConcreteModule.Builder(coords, null);
        final var parent = mock(ClassifiedComponent.class);
        final var modularity = mockModuleModularity();

        assertThatThrownBy(() -> builder.buildAsChild(parent, modularity))
                .isInstanceOf(ComponentPartitionNotDefinedException.class);
    }

    @Test
    void shouldThrowOnDuplicateDimensionInHierarchy() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var parent = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), testUtil.presentationPartition)
                .buildAsChild(root, mockModuleModularity());

        final var builder = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), testUtil.dbAccessPartition);

        assertThatThrownBy(() -> builder.buildAsChild(parent, mockModuleModularity()))
                .isInstanceOf(DuplicatePartitionClassificationException.class);
    }

    @Test
    void shouldThrowWhenPartitionIsNotAllowedByModularity() {
        final var parent = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();

        final var modularity = mockModuleModularity();
        final var parentModularity = mockModuleModularity();
        when(modularity.getParent()).thenReturn(Optional.of(parentModularity));
        final var allowedPartitions = mock(DimensionPartitionGroup.class);
        when(allowedPartitions.getPartitions()).thenReturn(Set.of(testUtil.presentationPartition));
        when(parentModularity.getAllowedPartitions()).thenReturn(allowedPartitions);

        final var builder = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), testUtil.presentationPartition);

        assertThatThrownBy(() -> builder.buildAsChild(parent, modularity))
                .isInstanceOf(PartitionNotAllowedException.class);
    }

    @Test
    void shouldReturnImmutableChildrenList() {
        final var root = new ClassifiedConcreteModule.Builder(mock(ModuleCoordinates.class), null).buildAsRoot();
        final var children = root.getChildren();

        assertThatThrownBy(() -> children.add(mock(ClassifiedComponent.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldReturnCoordinatesAndClassification() {
        final var coords = new ModuleCoordinates("io.group", "artifact");
        final var root = new ClassifiedConcreteModule.Builder(coords, null).buildAsRoot();

        assertThat(root.getModuleCoordinates()).isEqualTo(coords);
        assertThat(root.getClassification()).isNotNull();
    }

    @Test
    void shouldThrowWhenPartitionDimensionMatchesParentDimension() {
        final var parent = new ClassifiedConcreteModule.Builder(
                new ModuleCoordinates("g", "p"), null).buildAsRoot();
        final var childBuilder = new ClassifiedConcreteModule.Builder(
                new ModuleCoordinates("g", "c"), testUtil.dbAccessPartition);
        final var grandBuilder = new ClassifiedConcreteModule.Builder(
                new ModuleCoordinates("g", "gc"), testUtil.businessPartition);

        final var child = childBuilder.buildAsChild(parent, mockModuleModularity());

        assertThatThrownBy(() -> grandBuilder.buildAsChild(child, mockModuleModularity()))
                .isInstanceOf(DuplicatePartitionClassificationException.class);
    }
}
