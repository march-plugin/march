package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedConcreteModuleChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

public class ClassifiedConcreteModule extends ClassifiedModule {

    private final PackageHierarchy rootPackage;

    protected ClassifiedConcreteModule(final ModuleCoordinates moduleCoordinates, final Classification classification, final Dimension.Partition partition, final PackageHierarchy rootPackage) {
        super(moduleCoordinates, classification, partition);
        this.rootPackage = rootPackage;
    }

    public PackageHierarchy getRootPackage() {
        return rootPackage;
    }

    @Override
    protected void validateChildren() {
        final var children = getChildren();
        final var isCodeModule = children.getFirst() instanceof ClassifiedPackage;
        final var isInvalid = children.stream().anyMatch(child -> (child instanceof ClassifiedPackage) != isCodeModule);

        if (isInvalid) {
            throw new ClassifiedConcreteModuleChildrenException();
        }
    }

    public static class Builder extends ClassifiedModule.Builder<ClassifiedConcreteModule> {

        private PackageHierarchy rootPackage;

        /**
         * Constructs a builder for constructing a classified concrete module.
         *
         * @param moduleCoordinates the coordinates of the module
         */
        public Builder(final ModuleCoordinates moduleCoordinates) {
            super(moduleCoordinates);
        }

        /**
         * Sets the root package of the module.
         *
         * @param newRootPackage the root package to set.
         * @return the builder to enable chaining
         */
        public Builder setRootPackage(final PackageHierarchy newRootPackage) {
            this.rootPackage = newRootPackage;
            return this;
        }

        /**
         * Constructs the classified module as project root.
         *
         * @return the built classified module
         */
        public ClassifiedConcreteModule buildAsRoot() {
            final var classification = new Classification.Builder().build();
            return new ClassifiedConcreteModule(getCoordinates(), classification, null, null);
        }

        @Override
        protected ClassifiedConcreteModule build(final ClassifiedComponent parent, final Dimension.Partition partition) {
            return new ClassifiedConcreteModule(getCoordinates(), buildClassificationWithParent(parent, partition), partition, rootPackage);
        }

        @Override
        protected void validateConvention(final ModuleModularity moduleModularity, final Dimension.Partition partition, final ClassifiedComponent builtClassification) {
            new RootPackageConventionValidator().validate(moduleModularity.getConvention(), (placeHolder) -> replacePackageName(builtClassification, partition, placeHolder), rootPackage == null ? null : rootPackage);

            super.validateConvention(moduleModularity, partition, builtClassification);
        }

        private String replacePackageName(final ClassifiedComponent parent, final Dimension.Partition partition, final String value) {
            if (value.equals("groupId")) {
                return getCoordinates().groupId();
            }
            if (value.equals(partition.getDimension().getName())) {
                return partition.getName();
            }
            return parent.getClassification().getPartition(value).getName();
        }

    }
}