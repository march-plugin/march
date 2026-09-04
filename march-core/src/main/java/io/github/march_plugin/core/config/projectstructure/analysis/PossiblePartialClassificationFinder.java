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
        final var remaining = new HashSet<>(missingPartitions);
        final var wanted = remaining.stream().filter(partition -> partition.getDimension().equals(modularity.getDimension())).findFirst();

        if (wanted.isEmpty()) {
            final var result = new HashSet<Modularity>();
            for (final var child : modularity.getChildren()) {
                result.addAll(findPossibleLocationsOfPartialClassifications(child, remaining));
            }
            return result;
        }

        remaining.remove(wanted.get());

        if (remaining.isEmpty()) {
            final var res = new HashSet<Modularity>();
            if (modularity.getChildren().isEmpty()) {
                res.add(modularity);
            }
            for (final var child : modularity.getChildren()) {
                if (matchesCase(modularity, child, wanted.get())) {
                    res.add(child);
                    res.addAll(child.getAllChildren());
                }
            }
            return res;
        }

        final var result = new HashSet<Modularity>();
        for (final var child : modularity.getChildren()) {
            if (matchesCase(modularity, child, wanted.get())) {
                result.addAll(findPossibleLocationsOfPartialClassifications(child, new HashSet<>(remaining)));
            }
        }
        return result;
    }

    /**
     * Whether child can be classified with wanted: its own case contains it, or (case-less template) parent allows it.
     */
    private static boolean matchesCase(final Modularity parent, final Modularity child, final Dimension.Partition wanted) {
        if (child.getCasePartitions() != null) {
            return child.getCasePartitions().contains(wanted);
        }
        return parent.getAllowedPartitions() == null || parent.getAllowedPartitions().contains(wanted);
    }
}
