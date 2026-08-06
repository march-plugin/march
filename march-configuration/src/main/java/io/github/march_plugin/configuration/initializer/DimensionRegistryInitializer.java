package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.dimensions.DimensionDto;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;

import java.util.List;

/**
 * Builds a {@link DimensionRegistry} from the dimensions declared in the March configuration.
 */
public class DimensionRegistryInitializer {

    /**
     * Builds the dimension registry from the given dimension configuration entries.
     *
     * @param dimensions the dimensions declared in the March configuration.
     * @return the dimension registry populated with the configured dimensions and partitions
     */
    public DimensionRegistry build(final List<DimensionDto> dimensions) {
        final var registryBuilder = new DimensionRegistry.Builder();

        for (final var dimensionDto : dimensions) {
            final var dimensionBuilder = new Dimension.Builder(dimensionDto.name(), dimensionDto.description());

            dimensionDto.partitions().partitions().forEach(p -> dimensionBuilder.addPartition(p.name(), p.description()));
            registryBuilder.addDimension(dimensionBuilder.build());
        }

        return registryBuilder.build();
    }
}