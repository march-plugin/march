package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedVirtualModuleReferenceChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;

public class ClassifiedVirtualModuleReference extends ClassifiedModule {

    private final ModuleCoordinates externalModuleCoordinates;

    protected ClassifiedVirtualModuleReference(final ModuleCoordinates moduleCoordinates, final Classification classification, final Dimension.Partition partition, final ModuleCoordinates externalModuleCoordinates) {
        super(moduleCoordinates, classification, partition);
        this.externalModuleCoordinates = externalModuleCoordinates;
    }

    public ModuleCoordinates getExternalCoordinates() {
        return externalModuleCoordinates;
    }

    @Override
    protected void validateChildren() {
        throw new ClassifiedVirtualModuleReferenceChildrenException();
    }

    public static class Builder extends ClassifiedModule.Builder<ClassifiedVirtualModuleReference> {

        private final ModuleCoordinates externalModuleCoordinates;

        /**
         * Constructs a builder for constructing a classified virtual module reference.
         *
         * @param moduleCoordinates the coordinates of the module
         * @param externalModuleCoordinates the coordinates of the external module used in the project
         */
        public Builder(final ModuleCoordinates moduleCoordinates, final ModuleCoordinates externalModuleCoordinates) {
            super(moduleCoordinates);
            this.externalModuleCoordinates = externalModuleCoordinates;
        }

        @Override
        protected ClassifiedVirtualModuleReference build(final ClassifiedComponent parent, final Dimension.Partition partition) {
            return new ClassifiedVirtualModuleReference(getCoordinates(), buildClassificationWithParent(parent, partition), partition, externalModuleCoordinates);
        }
    }
}