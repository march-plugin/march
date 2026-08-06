package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ClassifiedVirtualModuleChildrenException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;

public class ClassifiedVirtualModule extends ClassifiedModule {

    protected ClassifiedVirtualModule(final ModuleCoordinates moduleCoordinates, final Classification classification, final Dimension.Partition partition) {
        super(moduleCoordinates, classification, partition);
    }

    @Override
    protected void validateChildren() {
        for (final var child : getChildren()) {
            if (!(child instanceof ClassifiedVirtualModule || child instanceof ClassifiedVirtualModuleReference)) {
                throw new ClassifiedVirtualModuleChildrenException();
            }
        }
    }

    public static class Builder extends ClassifiedModule.Builder<ClassifiedVirtualModule> {

        /**
         * Constructs a builder for constructing a classified virtual module.
         *
         * @param moduleCoordinates the coordinates of the module
         * @param partition the partition the module classifies additional to the classification of parent module
         */
        public Builder(final ModuleCoordinates moduleCoordinates, final Dimension.Partition partition) {
            super(moduleCoordinates, partition);
        }

        @Override
        protected ClassifiedVirtualModule build(final ClassifiedComponent parent) {
            return new ClassifiedVirtualModule(getCoordinates(), buildClassificationWithParent(parent), getPartition());
        }
    }
}