package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ComponentPartitionNotDefinedException;
import io.github.march_plugin.core.config.classification.exception.DuplicatePartitionClassificationException;
import io.github.march_plugin.core.config.classification.exception.PartitionNotAllowedException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages all classified components (modules and packages) of the project as tree structure.
 *
 * <p>The tree consists of the following component types:</p>
 * <ul>
 * <li>{@link ModuleModularity} - Represents a generic module.</li>
 * <li>{@link ClassifiedConcreteModule} - Represents a module within the current project.</li>
 * <li>{@link ClassifiedVirtualModuleReference} - Represents a module from a different project.</li>
 * <li>{@link ClassifiedVirtualModule} - A structural placeholder for organizing external project modules.</li>
 * <li>{@link PackageModularity} - Represents a package within the project.</li>
 * </ul>
 */
public abstract class ClassifiedComponent {
    private final ModuleCoordinates moduleCoordinates;
    private final Classification classification;
    private final Dimension.Partition partition;
    private final List<ClassifiedComponent> internalChildren;
    private final List<ClassifiedComponent> children;

    protected ClassifiedComponent(final ModuleCoordinates moduleCoordinates, final Classification classification, final Dimension.Partition partition) {
        this.moduleCoordinates = moduleCoordinates;
        this.classification = classification;
        this.partition = partition;
        internalChildren = new ArrayList<>();
        this.children = Collections.unmodifiableList(internalChildren);
    }

    public Dimension.Partition getPartition() {
        return partition;
    }

    public Classification getClassification() {
        return classification;
    }

    public ModuleCoordinates getModuleCoordinates() {
        return moduleCoordinates;
    }

    public List<ClassifiedComponent> getChildren() {
        return children;
    }

    private void addChild(final ClassifiedComponent child) {
        internalChildren.add(child);
        validateChildren();
    }

    protected abstract void validateChildren();

    public static abstract class Builder<T extends ClassifiedComponent, U extends Modularity> {

        private final ModuleCoordinates moduleCoordinates;

        /**
         * Constructs a builder for constructing a classified component.
         *
         * @param moduleCoordinates the coordinates of the component
         */
        public Builder(final ModuleCoordinates moduleCoordinates) {
            this.moduleCoordinates = moduleCoordinates;
        }

        protected ModuleCoordinates getCoordinates() {
            return moduleCoordinates;
        }

        protected Classification buildClassificationWithParent(final ClassifiedComponent parent, final Dimension.Partition partition) {
            return parent.getClassification().buildChild(partition);
        }

        /**
         * Constructs the classified component.
         *
         * @param parent the classified parent component
         * @param partition the partition the component classifies additional to the classification of parent component
         * @param modularity the modularity describing the component inside the project structure
         * @return the built classified component
         */
        public T buildAsChild(final ClassifiedComponent parent, final Dimension.Partition partition, final U modularity) {
            if (partition == null) {
                throw new ComponentPartitionNotDefinedException(moduleCoordinates.toString());
            }

            if (parent.getClassification().getPartitions().stream().map(Dimension.Partition::getDimension).toList().contains(partition.getDimension())) {
                throw new DuplicatePartitionClassificationException(partition.getDimension().getName());
            }

            final var classifiedComponent = build(parent, partition);

            if (modularity.getParent().isPresent()) {
                final var allowedPartitions = modularity.getParent().get().getAllowedPartitions();

                if (allowedPartitions != null && !allowedPartitions.contains(partition)) {
                    throw new PartitionNotAllowedException(partition.toString(), allowedPartitions.toString());
                }
            }

            validateConvention(modularity, partition, classifiedComponent);

            parent.addChild(classifiedComponent);
            return classifiedComponent;
        }

        protected abstract T build(final ClassifiedComponent parent, final Dimension.Partition partition);
        protected abstract void validateConvention(final U modularity, final Dimension.Partition partition, final ClassifiedComponent builtClassification);
    }
}