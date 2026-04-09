package io.github.march_plugin.core.dimensions.model;

import io.github.march_plugin.core.dimensions.exceptions.DimensionNotFoundException;
import io.github.march_plugin.core.dimensions.exceptions.DuplicationDimensionDefinitionException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry of the dimensions and partitions defined in the configuration.
 */
public final class DimensionRegistry {

    private final Map<String, Dimension> dimensions;

    private DimensionRegistry(final Map<String, Dimension> dimensions) {
        this.dimensions = Map.copyOf(dimensions);
    }

    /**
     * Gets the dimension object.
     *
     * @param name the name of the dimension.
     * @return the dimension object
     *
     * @throws DimensionNotFoundException if dimension is not defined in the configuration
     */
    public Dimension getDimension(final String name) {
        final var dimension = dimensions.get(name);

        if (dimension == null) {
            throw new DimensionNotFoundException(name);
        }

        return dimension;
    }

    /**
     * Gets all dimensions.
     *
     * @return all dimensions configured in the configuration
     */
    public Set<Dimension> getDimensions() {
        return new HashSet<>(dimensions.values());
    }

    public static class Builder {
        private final Map<String, Dimension> dimensions = new HashMap<>();

        /**
         * Adds a dimension to the registry.
         *
         * @param dimension the dimension to add to the registry
         *
         * @return the builder for chaining
         * @throws DuplicationDimensionDefinitionException if dimension is already defined in the registry
         */
        public Builder addDimension(final Dimension dimension) {
            if (dimensions.get(dimension.getName()) != null) {
                throw new DuplicationDimensionDefinitionException(dimension.getName());
            }
            dimensions.put(dimension.getName(), dimension);
            return this;
        }

        /**
         * Builds the registry.
         * @return the build registry
         */
        public DimensionRegistry build() {
            return new DimensionRegistry(dimensions);
        }
    }
}