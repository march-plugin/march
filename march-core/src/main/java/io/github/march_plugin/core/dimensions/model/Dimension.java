package io.github.march_plugin.core.dimensions.model;

import io.github.march_plugin.core.dimensions.exceptions.BlankDimensionNameException;
import io.github.march_plugin.core.dimensions.exceptions.BlankPartitionNameException;
import io.github.march_plugin.core.dimensions.exceptions.DuplicationPartitionDefinitionException;
import io.github.march_plugin.core.dimensions.exceptions.InvalidDimensionNameException;
import io.github.march_plugin.core.dimensions.exceptions.InvalidPartitionCountException;
import io.github.march_plugin.core.dimensions.exceptions.PartitionNotFoundException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class Dimension implements Comparable<Dimension> {

    private final String name;
    private final String description;
    private final Set<Partition> partitions;


    /**
     * Constructs the dimension.
     *
     * @param name        the name of the dimension.
     * @param description the description of the dimension.
     */
    private Dimension(final String name, final String description) {
        if (name == null || name.isBlank()) {
            throw new BlankDimensionNameException();
        }
        if (Set.of("source", "target").contains(name)) {
            throw new InvalidDimensionNameException(name);
        }

        this.name = name;
        this.description = description;
        this.partitions = new HashSet<>();
    }

    @Override
    public int compareTo(final Dimension o) {
        return this.name.compareTo(o.name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Partition> getPartitions() {
        return partitions;
    }

    /**
     * Gets the partitions of a dimension by name.
     *
     * @param partitionName the name of the partition
     * @return the partition with the given name
     */
    public Partition getPartition(final String partitionName) {
        return partitions.stream().filter(p -> p.getName().equals(partitionName)).findFirst().orElseThrow(() -> new PartitionNotFoundException(name, partitionName));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Dimension dimension) {
            return name.equals(dimension.name);
        }
        return false;
    }

    /**
     * Builder for creating dimensions including partitions.
     */
    public static class Builder {

        private final Dimension dimension;

        /**
         * Constructs the DimensionBuilder.
         *
         * @param name the name of the dimension.
         */
        public Builder(final String name) {
            this(name, null);
        }

        /**
         * Constructs the DimensionBuilder.
         *
         * @param name        the name of the dimension.
         * @param description the description of the dimension.
         */
        public Builder(final String name, final String description) {
            dimension = new Dimension(name, description);
        }

        /**
         * Adds a partition to the dimension.
         *
         * @param name the name of the partition.
         * @return the partition created
         */
        public Partition addPartition(final String name) {
            return addPartition(name, null);
        }

        /**
         * Adds a partition to the dimension.
         *
         * @param name        the name of the partition.
         * @param description the description of the partition.
         * @return the partition created
         */
        public Partition addPartition(final String name, final String description) {
            final var partition = new Partition(name, description, dimension);

            if (dimension.partitions.stream().anyMatch(p -> p.getName().equals(name))) {
                throw new DuplicationPartitionDefinitionException(dimension.getName(), name);
            }

            dimension.partitions.add(partition);
            return partition;
        }

        /**
         * Gets the dimension created.
         *
         * @return the dimension
         */
        public Dimension build() {
            if (dimension.partitions.size() < 2) {
                throw new InvalidPartitionCountException(dimension.name);
            }

            return dimension;
        }
    }

    public static final class Partition implements Comparable<Partition> {

        private final String name;
        private final String description;
        private final Dimension dimension;

        private Partition(final String name, final String description, final Dimension dimension) {
            if (name == null || name.isBlank()) {
                throw new BlankPartitionNameException();
            }
            Objects.requireNonNull(dimension, "Dimension cannot be null");

            this.name = name;
            this.description = description;
            this.dimension = dimension;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Dimension getDimension() {
            return dimension;
        }

        @Override
        public String toString() {
            return dimension.getName() + "." + name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension.getName(), name);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Partition other) {
                return dimension.equals(other.dimension) && name.equals(other.name);
            }
            return false;
        }

        @Override
        public int compareTo(final Partition other) {
            return this.name.compareTo(other.name);
        }
    }

}