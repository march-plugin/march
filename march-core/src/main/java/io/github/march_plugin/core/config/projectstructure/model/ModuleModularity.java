package io.github.march_plugin.core.config.projectstructure.model;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.exception.DifferentChildModularityTypeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the modularity of modules represented as tree structure.
 */
public final class ModuleModularity extends Modularity {

    private final List<ModuleModularity> childModules;
    private final ModuleConvention moduleConvention;

    private ModuleModularity(final Dimension dimension, final DimensionPartitionGroup allowedPartitions, final ModuleModularity parent, final DimensionPartitionGroup casePartitions, final ModuleConvention moduleConvention) {
        super(dimension, allowedPartitions, parent, casePartitions);
        this.childModules = new ArrayList<>();

        if (parent != null) {
            parent.addChildModuleModularity(this);
        }

        this.moduleConvention = moduleConvention;
    }

    public ModuleConvention getConvention() {
        return moduleConvention;
    }

    public List<ModuleModularity> getChildModules() {
        return childModules;
    }

    private void addChildModuleModularity(final ModuleModularity child) {
        childModules.add(child);
        validateChildren();
    }

    /**
     * Adds a child package modularity.
     *
     * @param child the child to add
     */
    @Override
    protected void addChildPackageModularity(final PackageModularity child) {
        if (!childModules.isEmpty()) {
            throw new DifferentChildModularityTypeException();
        }

        super.addChildPackageModularity(child);
    }

    @Override
    public List<? extends Modularity> getChildren() {
        return childModules.isEmpty() ? getChildPackages() : childModules;
    }

    @Override
    public String toString() {
        return "Module Modularity with convention: " + moduleConvention.toString();
    }

    public static class Builder extends Modularity.Builder<Builder> {
        private final ModuleConvention convention;

        /**
         * Constructs a builder for constructing a module modularity.
         *
         * @param dimension the dimension of the modularity
         * @param convention the conventions for modules defined by this modularity
         */
        public Builder(final Dimension dimension, final ModuleConvention convention) {
            super(dimension);
            this.convention = convention;
        }

        /**
         * Builds the module modularity as project root.
         *
         * @return the built instance
         */
        public ModuleModularity buildAsRoot() {
            return new ModuleModularity(getDimension(), getAllowedPartitions(), null, getCasePartitions(), convention);

        }

        /**
         * Builds the module modularity and registers it as child of the given parent.
         *
         * @param parent the parent modularity
         * @return the built instance
         */
        public ModuleModularity buildAsChild(final ModuleModularity parent) {
            return new ModuleModularity(getDimension(), getAllowedPartitions(), parent, getCasePartitions(), ModuleConvention.buildChild(parent.getConvention(), convention));

        }
    }
}
