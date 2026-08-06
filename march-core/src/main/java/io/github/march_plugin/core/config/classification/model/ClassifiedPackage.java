package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedPackageChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;

public class ClassifiedPackage extends ClassifiedComponent {

    private final PackageHierarchy packageHierarchy;
    private final boolean isOptional;
    private final PackageCoordinates packageCoordinates;

    protected ClassifiedPackage(final ModuleCoordinates coordinates, final Classification classification, final Dimension.Partition partition, final PackageHierarchy packageHierarchy, final boolean isOptional) {
        super(coordinates, classification, partition);
        this.packageHierarchy = packageHierarchy;
        this.packageCoordinates = new PackageCoordinates(coordinates, packageHierarchy);
        this.isOptional = isOptional;
    }

    public PackageHierarchy getPackageHierarchy() {
        return packageHierarchy;
    }

    public PackageCoordinates getPackageCoordinates() {
        return packageCoordinates;
    }

    public PackageClassification getClassifiedPackage() {
        return new PackageClassification(getClassification(), packageHierarchy, getChildren().isEmpty());
    }

    public boolean isOptional() {
        return isOptional;
    }

    @Override
    protected void validateChildren() {
        for (final var child : getChildren()) {
            if (!(child instanceof ClassifiedPackage)) {
                throw new ClassifiedPackageChildrenException();
            }
        }
    }

    public static class Builder extends ClassifiedComponent.Builder<ClassifiedPackage, PackageModularity> {

        private final PackageHierarchy packageHierarchy;
        private boolean isOptional;

        /**
         * Constructs a builder for constructing a classified package.
         *
         * @param moduleCoordinates the coordinates of the module of the package
         * @param partition the partition the package classifies additional to the classification of parent component
         * @param packageHierarchy the path of the package within its module
         */
        public Builder(final ModuleCoordinates moduleCoordinates, final Dimension.Partition partition, final PackageHierarchy packageHierarchy) {
            super(moduleCoordinates, partition);
            this.packageHierarchy = packageHierarchy;
            isOptional = false;
        }

        /**
         * Marks the classified package as optional. This allows packages in templates to not be mandatory in all modules using the template
         * @return the builder to enable chaining
         */
        public Builder setOptional() {
            isOptional = true;
            return this;
        }

        @Override
        protected ClassifiedPackage build(final ClassifiedComponent parent) {
            final var classification = buildClassificationWithParent(parent);
            return new ClassifiedPackage(getCoordinates(), classification, getPartition(), packageHierarchy, isOptional);
        }

        @Override
        protected void validateConvention(final PackageModularity packageModularity, final ClassifiedComponent builtClassification) {
            new PackageModularityConventionValidator().validate(packageModularity.getConvention(), (x) -> replacePackageName(builtClassification, x), packageHierarchy.getSimpleName());
        }

        private String replacePackageName(final ClassifiedComponent classifiedComponent, final String value) {
            if (value.equals(getPartition().getDimension().getName())) {
                return getPartition().getName();
            }
            return classifiedComponent.getClassification().getPartition(value).getName();
        }
    }
}
