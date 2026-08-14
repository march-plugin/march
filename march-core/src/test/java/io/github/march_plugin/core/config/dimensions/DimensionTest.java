package io.github.march_plugin.core.config.dimensions;

import io.github.march_plugin.core.config.dimensions.exceptions.BlankDimensionNameException;
import io.github.march_plugin.core.config.dimensions.exceptions.BlankPartitionNameException;
import io.github.march_plugin.core.config.dimensions.exceptions.DuplicationPartitionDefinitionException;
import io.github.march_plugin.core.config.dimensions.exceptions.InvalidDimensionNameException;
import io.github.march_plugin.core.config.dimensions.exceptions.InvalidPartitionCountException;
import io.github.march_plugin.core.config.dimensions.exceptions.PartitionNotFoundException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DimensionTest {

    @Test
    void shouldBuildValidDimension() {
        final var dimensionBuilder = new Dimension.Builder("layer", "Technical layer");
        final var initialServicePartition = dimensionBuilder.addPartition("service");
        dimensionBuilder.addPartition("database", "Database Layer");
        final var dimension = dimensionBuilder.build();

        assertThat(dimension.getName()).isEqualTo("layer");

        final var servicePartition = dimension.getPartition("service");
        assertThat(servicePartition).isEqualTo(initialServicePartition);
        assertThat(servicePartition.getName()).isEqualTo("service");
        assertThat(servicePartition.getDimension()).isEqualTo(dimension);
        assertThat(servicePartition.getDescription()).isNull();


        final var databasePartition = dimension.getPartition("database");
        assertThat(databasePartition.getName()).isEqualTo("database");
        assertThat(databasePartition.getDimension()).isEqualTo(dimension);
        assertThat(databasePartition.getDescription()).isEqualTo("Database Layer");
    }

    @Test
    void shouldNotAllowDuplicationPartitions() {
        final var dimensionBuilder = new Dimension.Builder("layer");
        dimensionBuilder.addPartition("service");

        assertThatThrownBy(() -> dimensionBuilder.addPartition("service"))
                .isInstanceOf(DuplicationPartitionDefinitionException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"source", "target"})
    void shouldNotAllowForbiddenDimensionNames(final String forbiddenName) {
        assertThatThrownBy(() -> new Dimension.Builder(forbiddenName))
                .isInstanceOf(InvalidDimensionNameException.class);
    }

    @Test
    void getPartitionShouldThrowIfPartitionIsNotDefined() {
        final var dimensionBuilder = new Dimension.Builder("layer", "Technical layer");
        dimensionBuilder.addPartition("service");
        dimensionBuilder.addPartition("db-access");
        final var dimension = dimensionBuilder.build();

        assertThatThrownBy(() -> dimension.getPartition("presentation"))
                .isInstanceOf(PartitionNotFoundException.class);
    }

    @Test
    void getPartitionsShouldPreserveDeclarationOrder() {
        final var dimensionBuilder = new Dimension.Builder("layer", "Technical layer");
        final var presentation = dimensionBuilder.addPartition("presentation");
        final var service = dimensionBuilder.addPartition("service");
        final var business = dimensionBuilder.addPartition("business");
        final var dbAccess = dimensionBuilder.addPartition("db-access");
        final var dimension = dimensionBuilder.build();

        assertThat(dimension.getPartitions()).containsExactly(presentation, service, business, dbAccess);
    }

    @Test
    void dimensionMustHaveAtLeastTwoPartitions() {
        final var dimensionBuilder = new Dimension.Builder("layer", "Technical layer");
        dimensionBuilder.addPartition("service");

        assertThatThrownBy(dimensionBuilder::build)
                .isInstanceOf(InvalidPartitionCountException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void dimensionNameMustNotBeEmpty(final String invalidName) {
        assertThatThrownBy(() -> new Dimension.Builder(invalidName))
                .isInstanceOf(BlankDimensionNameException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void partitionNameMustNotBeEmpty(final String invalidName) {
        final var dimensionBuilder = new Dimension.Builder("layer", "Technical layer");

        assertThatThrownBy(() -> dimensionBuilder.addPartition(invalidName))
                .isInstanceOf(BlankPartitionNameException.class);
    }
}
