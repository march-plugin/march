package io.github.march_plugin.core.config.projectstructure.analysis;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;

import java.util.HashSet;
import java.util.Set;

public class PossiblePartialClassificationFinder {

    /**
     * Finds every modularity node whose ancestor path could satisfy the given partial classification.
     *
     * @param modularity the modularity node to start searching from
     * @param missingPartitions the partitions of the partial classification not yet matched
     * @return every modularity node whose ancestor path can satisfy all of {@code missingPartitions}
     */
    public Set<Modularity> findPossibleLocationsOfPartialClassifications(final Modularity modularity, final Set<Dimension.Partition> missingPartitions) {
        final var partitionToRemove = missingPartitions.stream().filter(partition -> partition.getDimension().equals(modularity.getDimension())).findFirst();

        if (partitionToRemove.isPresent()) {
            missingPartitions.remove(partitionToRemove.get());
            if (missingPartitions.isEmpty()) {
                final var res = new HashSet<Modularity>();
                res.add(modularity);
                res.addAll(modularity.getAllChildren());
                return res;
            }
        }

        final var result = new HashSet<Modularity>();

        for (final var child : modularity.getChildren()) {
            result.addAll(findPossibleLocationsOfPartialClassifications(child, new HashSet<>(missingPartitions)));
        }
        return result;
    }
}
