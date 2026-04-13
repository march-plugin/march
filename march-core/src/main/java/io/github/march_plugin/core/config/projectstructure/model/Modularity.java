package io.github.march_plugin.core.config.projectstructure.model;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.exception.DuplicateCasePartitionException;
import io.github.march_plugin.core.config.projectstructure.exception.DuplicateDimensionInPathException;
import io.github.march_plugin.core.config.projectstructure.exception.EmptyModularityDimensionException;
import io.github.march_plugin.core.config.projectstructure.exception.NoCaseDefinedForMultipleChildrenException;
import io.github.march_plugin.core.config.projectstructure.exception.NoChildModularityCaseFoundException;
import io.github.march_plugin.core.config.projectstructure.exception.UnequalCaseDimensionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Describes the modularity of components (modules and packages) represented as tree structure.
 *
 * <p>
 * {@link #dimension} defines the dimension that child components must classify.
 * {@link #allowedPartitions} restricts the partitions of the dimension allowed.
 * </p>
 * <p>
 * {@link #parent} defines the modularity of the parent component
 * {@link #casePartitions} separates the modularity from siblings by classification of the parent module.
 * {@link #childPackages} defines the modularity of children
 * </p>
 */
public abstract class Modularity {

    private final Dimension dimension;
    private final DimensionPartitionGroup allowedPartitions;
    private final Modularity parent;
    private final DimensionPartitionGroup casePartitions;
    private final List<PackageModularity> childPackages;
    private final List<PackageModularity> childPackagesInternal;

    protected Modularity(final Dimension dimension, final DimensionPartitionGroup allowedPartitions, final Modularity parent, final DimensionPartitionGroup casePartitions) {
        this.dimension = dimension;
        this.casePartitions = casePartitions;
        this.allowedPartitions = allowedPartitions;
        this.parent = parent;
        this.childPackagesInternal = new ArrayList<>();
        this.childPackages = Collections.unmodifiableList(childPackagesInternal);
        validateDimensionPath();
    }

    public List<PackageModularity> getChildPackages() {
        return childPackages;
    }

    public Optional<Modularity> getParent() {
        return Optional.ofNullable(parent);
    }

    public Dimension getDimension() {
        return dimension;
    }

    public DimensionPartitionGroup getCasePartitions() {
        return casePartitions;
    }


    public DimensionPartitionGroup getAllowedPartitions() {
        return allowedPartitions;
    }

    /**
     * Returns the modularity of children.
     * @return the children
     */
    public abstract List<? extends Modularity> getChildren();

    /**
     * Finds the child modularity.
     *
     * @param partition the classified partition of the component on this modularity level.
     * @return the child modularity with matching partition or the single child if only one exists
     */
    public Modularity getChild(final Dimension.Partition partition) {
        final var children = getChildren();

        if (children.size() == 1 && children.getFirst().getCasePartitions() == null) {
            return children.getFirst();
        }

        return children.stream()
                .filter(c -> c.getCasePartitions().contains(partition))
                .findFirst()
                .orElseThrow(() -> new NoChildModularityCaseFoundException(partition.toString()));
    }

    /**
     * Gets all child levels recursively.
     *
     * @return all child modularities
     */
    public List<Modularity> getAllChildren() {
        final var result = new ArrayList<Modularity>();
        for (final Modularity child : getChildren()) {
            result.add(child);
            result.addAll(child.getAllChildren());
        }
        return result;
    }

    /**
     * Adds a child package modularity.
     *
     * @param child the child to add
     */
    protected void addChildPackageModularity(final PackageModularity child) {
        childPackagesInternal.add(child);
        validateChildren();
    }

    /**
     * Validates that the dimension of this modularity does not already appear
     * anywhere in the ancestor path from this node up to the root.
     *
     * @throws DuplicateDimensionInPathException if the same dimension is used by any ancestor
     */
    private void validateDimensionPath() {
        if (dimension == null) {
            return;
        }

        var ancestor = parent;
        while (ancestor != null) {
            if (dimension.equals(ancestor.getDimension())) {
                throw new DuplicateDimensionInPathException(dimension.getName());
            }
            ancestor = ancestor.parent;
        }
    }

    /**
     * Validates the configuration of all children.
     */
    protected void validateChildren() {
        if (getDimension() == null) {
            throw new EmptyModularityDimensionException();
        }

        final var children = getChildren();
        for (final var child : children) {
            if (child.getCasePartitions() != null && !child.getCasePartitions().getDimension().equals(dimension)) {
                throw new UnequalCaseDimensionException(child.getCasePartitions().getDimension().toString(), dimension.toString());
            }
        }

        if (children.size() > 1) {
            for (final var child : children) {
                if (child.getCasePartitions() == null) {
                    throw new NoCaseDefinedForMultipleChildrenException();
                }
            }

            final var duplicatePartitions = children.stream()
                    .map(x -> x.getCasePartitions().getPartitions())
                    .flatMap(Set::stream)
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                    .entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .toList();

            if (!duplicatePartitions.isEmpty()) {
                throw new DuplicateCasePartitionException(duplicatePartitions.getFirst().getName());
            }
        }
    }

    public abstract static class Builder<T extends Builder<T>> {
        private DimensionPartitionGroup casePartitions;
        private DimensionPartitionGroup allowedPartitions;
        private final Dimension dimension;

        /**
         * Constructs a builder for constructing a modularity.
         *
         * @param dimension the dimension of the modularity
         */
        public Builder(final Dimension dimension) {
            this.dimension = dimension;
        }

        /**
         * Sets the case partitions of the modularity.
         *
         * @param newCasePartitions the partitions to set
         * @return the builder to enable chaining
         */
        public T setCasePartitions(final DimensionPartitionGroup newCasePartitions) {
            this.casePartitions = newCasePartitions;
            return (T) this;
        }

        /**
         * Sets the restrictions of the partitions of the dimension allowed.
         *
         * @param newAllowedPartitions the partitions to set
         * @return the builder to enable chaining
         */
        public T setAllowedPartitions(final DimensionPartitionGroup newAllowedPartitions) {
            this.allowedPartitions = newAllowedPartitions;
            return (T) this;
        }

        public DimensionPartitionGroup getCasePartitions() {
            return casePartitions;
        }

        public DimensionPartitionGroup getAllowedPartitions() {
            return allowedPartitions;
        }

        public Dimension getDimension() {
            return dimension;
        }
    }
}
