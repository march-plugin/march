package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.DimensionNotClassifiedException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages a Set of Partitions defining the Classification of a module.
 */
public final class Classification {
    private final Set<Dimension.Partition> partitions;

    private Classification(final Set<Dimension.Partition> partitions) {
        this.partitions = Collections.unmodifiableSet(partitions);
    }

    /**
     * Returns the classified partition of a specific {@link Dimension}.
     *
     * @param dimension the dimension that should be classified
     * @return the classified dimension
     * @throws DimensionNotClassifiedException if the dimension is not classified
     */
    public Dimension.Partition getPartition(final Dimension dimension) {
        return partitions.stream()
                .filter(p -> p.getDimension().equals(dimension))
                .findFirst()
                .orElseThrow(() -> new DimensionNotClassifiedException(dimension.getName()));
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

    @Override
    public String toString() {
        return "[" + String.join(";", partitions.stream().map(Dimension.Partition::toString).toList()) + "]";
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

        /**
         * Builds a classification by combining the partitions of an existing classification with one additional partition.
         *
         * @param classification the classification to extend
         * @param childPartition the partition to add
         * @return the built child classification
         */
        public static Classification buildChildClassification(final Classification classification, final Dimension.Partition childPartition) {
            final var builder = new Builder();
            for (final var partition : classification.partitions) {
                builder.addPartition(partition);
            }
            builder.addPartition(childPartition);
            return builder.build();
        }
    }
}
