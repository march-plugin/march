package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        for (final var child : node.getChildren()) {
            if (child.getCasePartitions() == null) {
                collect(child, pathSoFar, result);
            } else {
                for (final var partition : child.getCasePartitions().getPartitions()) {
                    final var branchPath = new HashMap<>(pathSoFar);
                    branchPath.put(partition.getDimension(), partition);
                    collect(child, branchPath, result);
                }
            }
        }
    }
}
