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
         * @param partition the partition the module classifies additional to the classification of parent module
         */
        public Builder(final ModuleCoordinates moduleCoordinates, final Dimension.Partition partition) {
            super(moduleCoordinates, partition);
        }

        @Override
        protected void validateConvention(final ModuleModularity modularity, final ClassifiedComponent builtClassification) {
            new ModuleConventionValidator().validate(
                    modularity.getConvention(),
                    (String placeHolder) -> builtClassification.getClassification().getPartition(placeHolder).getName(),
                    builtClassification.getModuleCoordinates());
        }
    }
}
