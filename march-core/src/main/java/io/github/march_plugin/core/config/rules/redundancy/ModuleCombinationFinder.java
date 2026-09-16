package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds every classification that may exist defined by the project structure (=modularity tree).
 */
final class ModuleCombinationFinder {

    private ModuleCombinationFinder() {
    }

    /**
     * Finds every classification that may exist defined by the project structure (=modularity tree).
     *
     * @param root the root of the project structure's module tree
     * @return one map per tree level reached, from each dimension assigned on its path to the assigned partition
     */
    static List<Map<Dimension, Dimension.Partition>> findCombinations(final ModuleModularity root) {
        final var result = new ArrayList<Map<Dimension, Dimension.Partition>>();
        collect(root, Map.of(), result);
        return result;
    }

    private static void collect(final Modularity node, final Map<Dimension, Dimension.Partition> pathSoFar, final List<Map<Dimension, Dimension.Partition>> result) {
        result.add(pathSoFar);

        if (node.getDimension() == null) {
            return;
        }

        for (final var partition : ownPartitions(node)) {
            final var classifiedPath = withPartition(pathSoFar, node.getDimension(), partition);
            final var matchingChildren = node.getChildren().stream()
                    .filter(child -> child.getCasePartitions() == null || child.getCasePartitions().contains(partition))
                    .toList();

            if (matchingChildren.isEmpty()) {
                result.add(classifiedPath);
            } else {
                for (final var child : matchingChildren) {
                    collect(child, classifiedPath, result);
                }
            }
        }
    }

    private static Map<Dimension, Dimension.Partition> withPartition(final Map<Dimension, Dimension.Partition> pathSoFar, final Dimension dimension, final Dimension.Partition partition) {
        final var branchPath = new HashMap<>(pathSoFar);
        branchPath.put(dimension, partition);
        return branchPath;
    }

    private static Set<Dimension.Partition> ownPartitions(final Modularity node) {
        final var allowedPartitions = node.getAllowedPartitions();
        return allowedPartitions != null ? allowedPartitions.getPartitions() : node.getDimension().getPartitions();
    }
}
