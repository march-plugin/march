package io.github.march_plugin.core.config.projectstructure.analysis;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Determines, for a set of dimensions, which of them are structurally package-only.
 */
public class DimensionGranularityFinder {

    /**
     * Finds which of the given dimensions are package-only.
     *
     * @param projectStructureRoot the root of the project structure tree to search
     * @param dimensions the dimensions to classify
     * @return the subset of {@code dimensions} that are only ever consumed by package nodes
     */
    public Set<Dimension> findPackageOnlyDimensions(final ModuleModularity projectStructureRoot, final Set<Dimension> dimensions) {
        final var nodes = new ArrayList<Modularity>();
        nodes.add(projectStructureRoot);
        nodes.addAll(projectStructureRoot.getAllChildren());

        final var seenOnModule = new HashSet<Dimension>();
        final var seenOnPackage = new HashSet<Dimension>();

        for (final var node : nodes) {
            final var dimension = node.getDimension();
            final var children = node.getChildren();
            if (dimension == null || children.isEmpty()) {
                continue;
            }
            if (children.getFirst() instanceof PackageModularity) {
                seenOnPackage.add(dimension);
            } else {
                seenOnModule.add(dimension);
            }
        }

        final var packageOnly = new HashSet<>(seenOnPackage);
        packageOnly.removeAll(seenOnModule);
        packageOnly.retainAll(dimensions);
        return packageOnly;
    }
}
