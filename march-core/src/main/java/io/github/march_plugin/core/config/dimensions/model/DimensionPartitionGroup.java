package io.github.march_plugin.core.config.dimensions.model;

import io.github.march_plugin.core.config.dimensions.exceptions.EmptyDimensionPartitionGroupException;
import io.github.march_plugin.core.config.dimensions.exceptions.GroupDuplicationPartitionDefinitionException;
import io.github.march_plugin.core.config.dimensions.exceptions.InvalidPartitionComparisonException;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores a set of partitions of the same dimension.
 */
public final class DimensionPartitionGroup {
    private final Dimension dimension;
    private final Set<Dimension.Partition> partitions;

    private DimensionPartitionGroup(final Dimension dimension, final Set<Dimension.Partition> partitions) {
        this.dimension = dimension;
        this.partitions = Set.copyOf(partitions);
    }

    public Set<Dimension.Partition> getPartitions() {
        return partitions;
    }

    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Checks if the group contains a partition .
     *
     * @param partition the partition to check
     * @return true if the group contains the partition
     */
    public boolean contains(final Dimension.Partition partition) {
        if (!partition.getDimension().equals(dimension)) {
            throw new InvalidPartitionComparisonException(dimension.getName(), partition.getName());
        }

        return partitions.contains(partition);
    }

    public static class Builder {
        private Dimension dimension;
        private final Set<Dimension.Partition> partitions = new HashSet<>();

        /**
         * Adds a partition to the classification.
         *
         * @param partitionToAdd the partition to add.
         * @return the builder for chaining
         */
        public Builder addPartition(final Dimension.Partition partitionToAdd) {
            if (this.dimension == null) {
                this.dimension = partitionToAdd.getDimension();
            } else if (!partitionToAdd.getDimension().equals(this.dimension)) {
                throw new InvalidPartitionComparisonException(dimension.getName(), partitionToAdd.getName());
            }

            if (!partitions.add(partitionToAdd)) {
                throw new GroupDuplicationPartitionDefinitionException(partitionToAdd.getName());
            }

            return this;
        }

        /**
         * Builds the group of partitions.
         *
         * @return the built group
         */
        public DimensionPartitionGroup build() {
            if (dimension == null) {
                throw new EmptyDimensionPartitionGroupException();
            }
            return new DimensionPartitionGroup(dimension, partitions);
        }
    }
}
