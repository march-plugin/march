package io.github.march_plugin.core.config.projectstructure.analysis;


import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds every combination of partitions, across a given set of dimensions, that the project structure
 * structurally admits.
 */
public class PossibleCombinationsFinder {

    /**
     * Finds all possible combinations of the given dimensions' partitions, as permitted by the shape of the project structure.
     *
     * @param modularity the root of the project structure tree to search
     * @param dimensions the dimensions to find combinations for; every returned combination has exactly one partition per dimension in this set
     * @return every distinct combination found
     */
    public List<Set<Dimension.Partition>> findCombinations(final ModuleModularity modularity, final Set<Dimension> dimensions) {
        final var possibleCombinations = new PossibleCombinationsRegistry();
        findCombinations(modularity, new Node(null, null, new ArrayList<>(), null), dimensions, possibleCombinations);
        return possibleCombinations.getCombinations();
    }

    private void findCombinations(final Modularity modularity, final Node parentNode, final Set<Dimension> dimensionsLeft, final PossibleCombinationsRegistry possibleCombinationsRegistry) {
        var actualParent = parentNode;

        if (modularity.getCasePartitions() != null && modularity.getParent().get().getDimension().equals(parentNode.dimension)) {
            final var newNode = new Node(parentNode.dimension, modularity.getCasePartitions(), new ArrayList<>(), parentNode.parent);
            parentNode.parent().children.add(newNode);
            actualParent = newNode;
        }

        if (modularity.getDimension() != null && dimensionsLeft.contains(modularity.getDimension())) {
            final var node = new Node(modularity.getDimension(), reachablePartitions(modularity), new ArrayList<>(), actualParent);
            actualParent.children.add(node);

            final var newDimensionsLeft = dimensionsLeft.stream().filter(d -> d != modularity.getDimension()).collect(Collectors.toCollection(HashSet::new));

            if (newDimensionsLeft.isEmpty()) {
                for (final var combination : node.getAllCombinations()) {
                    possibleCombinationsRegistry.addCombination(combination);
                }
            } else {
                for (final var childLevel : modularity.getChildren()) {
                    findCombinations(childLevel, node, newDimensionsLeft, possibleCombinationsRegistry);
                }
            }
        } else {
            for (final var childLevel : modularity.getChildren()) {
                findCombinations(childLevel, actualParent, dimensionsLeft, possibleCombinationsRegistry);
            }
        }
    }

    /**
     * Partitions of modularity's own dimension actually reachable through its children, not every declared one.
     */
    private DimensionPartitionGroup reachablePartitions(final Modularity modularity) {
        final var declaredByChildren = modularity.getChildren().stream()
                .map(Modularity::getCasePartitions)
                .filter(Objects::nonNull)
                .flatMap(group -> group.getPartitions().stream())
                .collect(Collectors.toSet());

        if (declaredByChildren.isEmpty()) {
            return modularity.getAllowedPartitions() != null
                    ? modularity.getAllowedPartitions()
                    : DimensionPartitionGroup.Builder.of(modularity.getDimension());
        }

        final var builder = new DimensionPartitionGroup.Builder();
        declaredByChildren.forEach(builder::addPartition);
        return builder.build();
    }

    private record Node(
            Dimension dimension,
            DimensionPartitionGroup partitions,
            List<Node> children,
            Node parent
    ) {

        /**
         * Evaluates all possible Combinations of Partitions possible by project structure.
         *
         * @return all possible combinations
         */
        public List<Set<Dimension.Partition>> getAllCombinations() {

            final var result = new ArrayList<Set<Dimension.Partition>>();
            if (parent.parent == null) {
                for (final var partition : partitions.getPartitions()) {
                    result.add(Set.of(partition));
                }
            } else {
                final var parentList = parent.getAllCombinations();

                for (final var parentSet : parentList) {
                    for (final var partition : partitions.getPartitions()) {
                        final var set = new HashSet<>(parentSet);
                        set.add(partition);
                        result.add(set);
                    }
                }
            }
            return result;
        }
    }

    private static final class PossibleCombinationsRegistry {
        private final List<Map<Dimension, Dimension.Partition>> combinations = new ArrayList<>();

        /**
         * Returns all possible combinations of Partitions possible by project structure.
         *
         * @return all combinations
         */
        public List<Set<Dimension.Partition>> getCombinations() {
            return combinations.stream().map(x -> x.values().stream().collect(Collectors.toSet())).toList();
        }

        /**
         * Add a possible combinations to the registry .
         *
         * @param partitions the combination to add
         */
        public void addCombination(final Set<Dimension.Partition> partitions) {
            var combinationExists = false;
            for (final var combination : combinations) {
                var match = true;
                for (final var partition : partitions) {
                    if (!combination.get(partition.getDimension()).equals(partition)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    combinationExists = true;
                    break;
                }
            }
            if (!combinationExists) {
                final var newEntry = new HashMap<Dimension, Dimension.Partition>();
                for (final var partition : partitions) {
                    newEntry.put(partition.getDimension(), partition);
                }
                combinations.add(newEntry);
            }
        }
    }
}
