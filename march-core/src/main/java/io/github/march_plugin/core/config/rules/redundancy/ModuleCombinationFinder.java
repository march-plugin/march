package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
    public static List<Map<Dimension, Dimension.Partition>> findCombinations(final ModuleModularity root) {
        return classifications(collect(root, child -> true));
    }

    /**
     * Finds every classification reachable purely through {@link ModuleModularity} nodes.
     *
     * @param root the root of the project structure's module tree
     * @return one map per module tree level reached, from each dimension assigned on its path to the assigned partition
     */
    public static List<Map<Dimension, Dimension.Partition>> findModuleCombinations(final ModuleModularity root) {
        return classifications(collect(root, child -> child instanceof ModuleModularity));
    }

    /**
     * Finds every leaf classification.
     *
     * @param root the root of the project structure's module tree
     * @return one map per leaf reached, from each dimension assigned on its path to the assigned partition
     */
    public static List<Map<Dimension, Dimension.Partition>> findLeafCombinations(final ModuleModularity root) {
        return leafClassifications(collect(root, child -> true));
    }

    /**
     * Finds every leaf classification reachable purely through {@link ModuleModularity} nodes.
     *
     * @param root the root of the project structure's module tree
     * @return one map per module leaf reached, from each dimension assigned on its path to the assigned partition
     */
    public static List<Map<Dimension, Dimension.Partition>> findLeafModuleCombinations(final ModuleModularity root) {
        return leafClassifications(collect(root, child -> child instanceof ModuleModularity));
    }

    private static List<Map<Dimension, Dimension.Partition>> classifications(final List<Combination> combinations) {
        return combinations.stream().map(Combination::classification).toList();
    }

    private static List<Map<Dimension, Dimension.Partition>> leafClassifications(final List<Combination> combinations) {
        return combinations.stream().filter(Combination::isLeaf).map(Combination::classification).toList();
    }

    private static List<Combination> collect(final ModuleModularity root, final Predicate<Modularity> childFilter) {
        final var result = new ArrayList<Combination>();
        collect(root, Map.of(), result, childFilter);
        return result;
    }

    private static void collect(final Modularity node, final Map<Dimension, Dimension.Partition> pathSoFar, final List<Combination> result, final Predicate<Modularity> childFilter) {
        result.add(new Combination(pathSoFar, isLeafNode(node, childFilter)));

        if (node.getDimension() == null) {
            return;
        }

        for (final var partition : ownPartitions(node)) {
            final var classifiedPath = withPartition(pathSoFar, node.getDimension(), partition);
            final var allMatchingChildren = node.getChildren().stream()
                    .filter(child -> child.getCasePartitions() == null || child.getCasePartitions().contains(partition))
                    .toList();
            final var matchingChildrenOfWantedType = allMatchingChildren.stream().filter(childFilter).toList();

            if (!allMatchingChildren.isEmpty() && matchingChildrenOfWantedType.isEmpty()) {
                continue;
            }

            if (matchingChildrenOfWantedType.isEmpty()) {
                result.add(new Combination(classifiedPath, true));
            } else {
                for (final var child : matchingChildrenOfWantedType) {
                    collect(child, classifiedPath, result, childFilter);
                }
            }
        }
    }

    private static boolean isLeafNode(final Modularity node, final Predicate<Modularity> childFilter) {
        if (node.getDimension() == null) {
            return true;
        }
        final var children = node.getChildren();
        return !children.isEmpty() && !childFilter.test(children.getFirst());
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

    private record Combination(Map<Dimension, Dimension.Partition> classification, boolean isLeaf) {
    }
}
