package io.github.march_plugin.core.config.dimensions;

import io.github.march_plugin.core.config.dimensions.exceptions.EmptyDimensionPartitionGroupException;
import io.github.march_plugin.core.config.dimensions.exceptions.GroupDuplicationPartitionDefinitionException;
import io.github.march_plugin.core.config.dimensions.exceptions.InvalidPartitionComparisonException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DimensionPartitionGroupTest {

    private Dimension.Partition service;
    private Dimension.Partition web;
    private Dimension.Partition persistence;
    private Dimension.Partition checkout;

    @BeforeEach
    void setUp() {
        // Build Layer Dimension and store its partitions
        final var layerBuilder = new Dimension.Builder("layer");
        service = layerBuilder.addPartition("service");
        web = layerBuilder.addPartition("web");
        persistence = layerBuilder.addPartition("persistence");

        // Build Team Dimension and store its partitions
        final var teamBuilder = new Dimension.Builder("team");
        checkout = teamBuilder.addPartition("checkout");
        teamBuilder.addPartition("search");
    }

    @Test
    void shouldBuildValidPartitionGroup() {
        final var group = new DimensionPartitionGroup.Builder()
                .addPartition(service)
                .addPartition(web)
                .build();

        assertThat(group.getPartitions()).containsExactlyInAnyOrder(service, web);
        assertThat(group.contains(service)).isTrue();
        assertThat(group.contains(web)).isTrue();
        assertThat(group.contains(persistence)).isFalse();
    }

    @Test
    void shouldNotAllowAddingPartitionsFromDifferentDimensionsInBuilder() {
        final var builder = new DimensionPartitionGroup.Builder().addPartition(service);

        assertThatThrownBy(() -> builder.addPartition(checkout))
                .isInstanceOf(InvalidPartitionComparisonException.class);
    }

    @Test
    void shouldNotAllowDuplicatePartitionsInBuilder() {
        final var builder = new DimensionPartitionGroup.Builder().addPartition(service);

        assertThatThrownBy(() -> builder.addPartition(service))
                .isInstanceOf(GroupDuplicationPartitionDefinitionException.class);
    }

    @Test
    void containsShouldThrowWhenCheckingPartitionFromDifferentDimension() {
        final var group = new DimensionPartitionGroup.Builder()
                .addPartition(service)
                .build();

        assertThatThrownBy(() -> group.contains(checkout))
                .isInstanceOf(InvalidPartitionComparisonException.class);
    }

    @Test
    void shouldHandleSinglePartitionGroup() {
        final var group = new DimensionPartitionGroup.Builder()
                .addPartition(service)
                .build();

        assertThat(group.getPartitions()).hasSize(1);
        assertThat(group.contains(service)).isTrue();
    }

    @Test
    void builderShouldThrowNullPointerExceptionIfAddingNull() {
        final var builder = new DimensionPartitionGroup.Builder();

        assertThatThrownBy(() -> builder.addPartition(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderShouldThrowIfNoPartitionIsAdded() {
        final var builder = new DimensionPartitionGroup.Builder();

        assertThatThrownBy(builder::build)
                .isInstanceOf(EmptyDimensionPartitionGroupException.class);
    }
}