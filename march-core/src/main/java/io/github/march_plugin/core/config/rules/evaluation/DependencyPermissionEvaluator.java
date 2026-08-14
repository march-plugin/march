package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.analysis.PossiblePartialClassificationFinder;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;

import java.util.HashSet;
import java.util.Set;

public abstract class DependencyPermissionEvaluator {

    private final ModuleModularity projectStructureRoot;

    /**
     * Constructs the evaluator.
     *
     * @param projectStructureRoot the root of the project structure tree, used to determine which
     *                             dimensions can structurally never apply to a given partial classification
     */
    public DependencyPermissionEvaluator(final ModuleModularity projectStructureRoot) {
        this.projectStructureRoot = projectStructureRoot;
    }

    /**
     * Evaluates a partially classified dependency.
     *
     * @param ruleRegistry the configured rules to evaluate against
     * @param dimensionRegistry all configured dimensions, used to find every dimension the given partitions leave unresolved
     * @param source The partially classified dependency source
     * @param target The partially classified dependency target
     *
     * @return The reduced Permission.
     */
    public abstract DependencyPermission reduce(final RuleRegistry ruleRegistry, final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target);

    protected Set<Dimension> getAlwaysNullDimensions(final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> partitions) {
        final var res = new HashSet<Dimension>();
        final var possibleLocations = new PossiblePartialClassificationFinder().findPossibleLocationsOfPartialClassifications(projectStructureRoot, partitions);

        for (final var dimension : dimensionRegistry.getDimensions()) {
            if (partitions.stream().anyMatch(p -> p.getDimension().equals(dimension))) {
                continue;
            }

            var isDimensionPresentInAnyPossibleDirectory = false;
            for (final var possibleLocation : possibleLocations) {
                var currentModularity = possibleLocation;

                while (currentModularity != null) {
                    if (currentModularity.getDimension() == dimension) {
                        isDimensionPresentInAnyPossibleDirectory = true;
                        break;
                    }
                    currentModularity = currentModularity.getParent().orElse(null);
                }

                if (isDimensionPresentInAnyPossibleDirectory) {
                    break;
                }
            }
            if (!isDimensionPresentInAnyPossibleDirectory) {
                res.add(dimension);
            }
        }
        return res;
    }
}
