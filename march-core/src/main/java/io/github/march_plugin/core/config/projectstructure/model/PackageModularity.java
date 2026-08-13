package io.github.march_plugin.core.config.projectstructure.model;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;

import java.util.List;

/**
 * Describes the modularity of packages represented as tree structure.
 */
public final class PackageModularity extends Modularity {
    private final PackageConvention convention;

    private PackageModularity(final Dimension dimension, final DimensionPartitionGroup allowedPartitions, final Modularity parent, final DimensionPartitionGroup casePartitions, final PackageConvention convention) {
        super(dimension, allowedPartitions, parent, casePartitions);
        parent.addChildPackageModularity(this);
        this.convention = convention;
    }

    public PackageConvention getConvention() {
        return convention;
    }

    @Override
    public List<? extends Modularity> getChildren() {
        return getChildPackages();
    }

    @Override
    public String toString() {
        return "Package Modularity with convention: " + convention.toString();
    }

    public static class Builder extends Modularity.Builder<Builder> {
        private final PackageConvention convention;

        /**
         * Constructs a builder for constructing a package modularity.
         *
         * @param dimension the dimension of the modularity
         * @param convention the conventions for packages defined by this modularity
         */
        public Builder(final Dimension dimension, final PackageConvention convention) {
            super(dimension);
            this.convention = convention;
        }

        /**
         * Builds the package modularity and registers it as child of the given parent.
         *
         * @param parent the parent modularity
         * @return the built instance
         */
        public PackageModularity buildAsChild(final Modularity parent) {
            return new PackageModularity(getDimension(), getAllowedPartitions(), parent, getCasePartitions(), convention);
        }
    }
}
