package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;

public abstract class ClassifiedModule extends ClassifiedComponent {

    protected ClassifiedModule(final ModuleCoordinates moduleCoordinates, final Classification classification, final Dimension.Partition partition) {
        super(moduleCoordinates, classification, partition);
    }

    /**
     * Whether this module is a leaf in the module hierarchy, meaning it has no child modules (it may still
     * have classified packages of its own).
     *
     * @return {@code true} if none of this module's children are themselves a module
     */
    public boolean isLeafModule() {
        return getChildren().stream().noneMatch(ClassifiedModule.class::isInstance);
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
