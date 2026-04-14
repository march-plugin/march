package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;

public abstract class ClassifiedModule extends ClassifiedComponent {

    protected ClassifiedModule(final ModuleCoordinates moduleCoordinates, final Classification classification, final Dimension.Partition partition) {
        super(moduleCoordinates, classification, partition);
    }

    public static abstract class Builder<T extends ClassifiedModule> extends ClassifiedComponent.Builder<T, ModuleModularity> {

        /**
         * Constructs a builder for constructing a classified module.
         *
         * @param moduleCoordinates the coordinates of the module
         */
        public Builder(final ModuleCoordinates moduleCoordinates) {
            super(moduleCoordinates);
        }

        @Override
        protected void validateConvention(final ModuleModularity modularity, final Dimension.Partition partition, final ClassifiedComponent builtClassification) {
            new ModuleConventionValidator().validate(
                    modularity.getConvention(),
                    (String placeHolder) -> builtClassification.getClassification().getPartition(placeHolder).getName(),
                    builtClassification.getModuleCoordinates());
        }
    }
}
