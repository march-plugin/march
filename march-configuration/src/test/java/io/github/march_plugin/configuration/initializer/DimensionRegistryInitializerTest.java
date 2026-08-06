package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.dimensions.DimensionDto;
import io.github.march_plugin.configuration.dto.dimensions.PartitionDto;
import io.github.march_plugin.configuration.dto.dimensions.PartitionsDto;
import io.github.march_plugin.core.config.dimensions.exceptions.BlankDimensionNameException;
import io.github.march_plugin.core.config.dimensions.exceptions.BlankPartitionNameException;
import io.github.march_plugin.core.config.dimensions.exceptions.DuplicationDimensionDefinitionException;
import io.github.march_plugin.core.config.dimensions.exceptions.DuplicationPartitionDefinitionException;
import io.github.march_plugin.core.config.dimensions.exceptions.InvalidDimensionNameException;
import io.github.march_plugin.core.config.dimensions.exceptions.InvalidPartitionCountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DimensionRegistryInitializerTest {

    private final DimensionRegistryInitializer initializer = new DimensionRegistryInitializer();


    @Test
    void shouldBuildRegistryWithSingleDimensionAndPartitions() {
        final var dimensionDto = dimension("layer", "Technical layer",
                partition("service", "Service layer"),
                partition("database", "Database layer"));

        final var registry = initializer.build(List.of(dimensionDto));

        final var dimension = registry.getDimension("layer");
        assertThat(dimension.getName()).isEqualTo("layer");
        assertThat(dimension.getDescription()).isEqualTo("Technical layer");
        assertThat(dimension.getPartitions()).hasSize(2);

        final var servicePartition = dimension.getPartition("service");
        assertThat(servicePartition.getDescription()).isEqualTo("Service layer");

        final var databasePartition = dimension.getPartition("database");
        assertThat(databasePartition.getDescription()).isEqualTo("Database layer");
    }

    @Test
    void shouldBuildRegistryWithMultipleDimensions() {
        final var layerDimension = dimension("layer", "Technical layer",
                partition("service", null),
                partition("database", null));
        final var moduleDimension = dimension("module", "Business module",
                partition("billing", null),
                partition("shipping", null));

        final var registry = initializer.build(List.of(layerDimension, moduleDimension));

        assertThat(registry.getDimensions()).hasSize(2);
        assertThat(registry.getDimension("layer").getName()).isEqualTo("layer");
        assertThat(registry.getDimension("module").getName()).isEqualTo("module");
    }

    @Test
    void shouldAllowPartitionWithoutDescription() {
        final var dimensionDto = dimension("layer", "Technical layer",
                partition("service", null),
                partition("database", null));

        final var registry = initializer.build(List.of(dimensionDto));

        assertThat(registry.getDimension("layer").getPartition("service").getDescription()).isNull();
    }

    @Test
    void shouldThrowWhenDimensionNameIsDuplicated() {
        final var firstDeclaration = dimension("layer", "Technical layer",
                partition("service", null), partition("database", null));
        final var secondDeclaration = dimension("layer", "Duplicate layer",
                partition("service", null), partition("database", null));

        assertThatThrownBy(() -> initializer.build(List.of(firstDeclaration, secondDeclaration)))
                .isInstanceOf(DuplicationDimensionDefinitionException.class);
    }

    @Test
    void shouldThrowWhenPartitionNameIsDuplicatedWithinDimension() {
        final var dimensionDto = dimension("layer", "Technical layer",
                partition("service", null), partition("service", null));

        assertThatThrownBy(() -> initializer.build(List.of(dimensionDto)))
                .isInstanceOf(DuplicationPartitionDefinitionException.class);
    }

    @Test
    void shouldThrowWhenDimensionHasFewerThanTwoPartitions() {
        final var dimensionDto = dimension("layer", "Technical layer", partition("service", null));

        assertThatThrownBy(() -> initializer.build(List.of(dimensionDto)))
                .isInstanceOf(InvalidPartitionCountException.class);
    }

    @Test
    void shouldThrowWhenDimensionHasNoPartitions() {
        final var dimensionDto = dimension("layer", "Technical layer");

        assertThatThrownBy(() -> initializer.build(List.of(dimensionDto)))
                .isInstanceOf(InvalidPartitionCountException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "target"})
    void shouldThrowWhenDimensionNameIsForbidden(final String forbiddenName) {
        final var dimensionDto = dimension(forbiddenName, "Technical layer",
                partition("service", null), partition("database", null));

        assertThatThrownBy(() -> initializer.build(List.of(dimensionDto)))
                .isInstanceOf(InvalidDimensionNameException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldThrowWhenDimensionNameIsBlank(final String invalidName) {
        final var dimensionDto = dimension(invalidName, "Technical layer",
                partition("service", null), partition("database", null));

        assertThatThrownBy(() -> initializer.build(List.of(dimensionDto)))
                .isInstanceOf(BlankDimensionNameException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldThrowWhenPartitionNameIsBlank(final String invalidName) {
        final var dimensionDto = dimension("layer", "Technical layer",
                partition(invalidName, null), partition("database", null));

        assertThatThrownBy(() -> initializer.build(List.of(dimensionDto)))
                .isInstanceOf(BlankPartitionNameException.class);
    }

    private static DimensionDto dimension(final String name, final String description, final PartitionDto... partitions) {
        return new DimensionDto(name, description, new PartitionsDto(List.of(partitions)));
    }

    private static PartitionDto partition(final String name, final String description) {
        return new PartitionDto(name, description);
    }
}
