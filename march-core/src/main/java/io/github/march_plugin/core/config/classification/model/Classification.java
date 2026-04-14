package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.DimensionNotClassifiedException;
import io.github.march_plugin.core.config.classification.exception.DuplicatePartitionClassificationException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages a Set of Partitions defining the Classification of a module.
 */
public final class Classification {
    private final Set<Dimension.Partition> partitions;
    private final Map<Dimension, Dimension.Partition> dimensionMap;

    private Classification(final Set<Dimension.Partition> partitions) {
        final var map = new HashMap<Dimension, Dimension.Partition>();
        for (final var partition : partitions) {
            if (map.put(partition.getDimension(), partition) != null) {
                throw new DuplicatePartitionClassificationException(partition.getDimension().getName());
            }
        }
        this.partitions = Collections.unmodifiableSet(new HashSet<>(partitions));
        this.dimensionMap = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the classified partition of a specific {@link Dimension}.
     *
     * @param dimension the dimension that should be classified
     * @return the classified dimension
     * @throws DimensionNotClassifiedException if the dimension is not classified
     */
    public Dimension.Partition getPartition(final Dimension dimension) {
        final var partition = dimensionMap.get(dimension);
        if (partition == null) {
            throw new DimensionNotClassifiedException(dimension.getName());
        }
        return partition;
    }

    /**
     * Returns the classified partition of a specific {@link Dimension}.
     *
     * @param dimensionName the name of the dimension that should be classified
     * @return the classified dimension
     * @throws DimensionNotClassifiedException if the dimension is not classified
     */
    public Dimension.Partition getPartition(final String dimensionName) {
        return partitions.stream()
                .filter(p -> p.getDimension().getName().equals(dimensionName))
                .findFirst()
                .orElseThrow(() -> new DimensionNotClassifiedException(dimensionName));
    }

    /**
     * Builds a classification by using the existing classification and adding one partition.
     *
     * @param childPartition the partition to add
     * @return the build child classification
     */
    public Classification buildChild(final Dimension.Partition childPartition) {
        if (partitions.stream().map(Dimension.Partition::getDimension).anyMatch(p -> p.equals(childPartition.getDimension()))) {
            throw new DuplicatePartitionClassificationException(childPartition.getDimension().getName());
        }

        final var builder = new Builder();
        for (final var partition : partitions) {
            builder.addPartition(partition);
        }
        builder.addPartition(childPartition);
        return builder.build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (Classification) o;
        return Objects.equals(partitions, that.partitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitions);
    }

    public Set<Dimension.Partition> getPartitions() {
        return partitions;
    }

    public static class Builder {
        private final Set<Dimension.Partition> partitions = new HashSet<>();

        /**
         * Adds a partition to the classification.
         * @param partition the partition to add.
         *
         * @return the builder for chaining
         */
        public Builder addPartition(final Dimension.Partition partition) {
            partitions.add(partition);
            return this;
        }

        /**
         * Builds the classification.
         * @return the built classification
         */
        public Classification build() {
            return new Classification(partitions);
        }
    }
}
