package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.analysis.PossiblePartialClassificationFinder;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;
import io.github.march_plugin.core.config.rules.model.Rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
     * Evaluates a partially classified package-level dependency.
     *
     * @param ruleRegistry all configured rules
     * @param dimensionRegistry all configured dimensions, used to find every dimension the given partitions leave unresolved
     * @param source The partially classified dependency source
     * @param target The partially classified dependency target
     *
     * @return The reduced Permission.
     */
    public abstract DependencyPermission reduce(final RuleRegistry ruleRegistry, final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target);

    /**
     * Reduces every non-module_only rule against source/target, using forced dimension values for what the partial classification leaves unresolved.
     */
    protected List<EvaluatedLogicalExpression> evaluateRules(final RuleRegistry ruleRegistry, final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target) {
        final var reducer = new RuleReducer();
        final var sourceForcedValues = getForcedDimensionValues(dimensionRegistry, source);
        final var targetForcedValues = getForcedDimensionValues(dimensionRegistry, target);

        return ruleRegistry.getRules().stream()
                .filter(rule -> !rule.ruleScope().equals(Rule.RuleScope.MODULE_ONLY))
                .map(r -> reducer.reduce(r.definition(), source, target, sourceForcedValues, targetForcedValues))
                .toList();
    }

    /**
     * Finds, for every dimension not part of {@code partitions}, whether it is structurally forced to one
     * single value (or forced always absent) at every location consistent with {@code partitions}; a
     * dimension left out of the result genuinely varies and must stay unresolved for the reduction.
     *
     * @param dimensionRegistry all configured dimensions
     * @param partitions the partial classification to find forced values for
     * @return a map from dimension to its single forced value; a present key mapped to {@code null} means the
     *         dimension is forced to always be absent
     */
    protected Map<Dimension, Dimension.Partition> getForcedDimensionValues(final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> partitions) {
        final var result = new HashMap<Dimension, Dimension.Partition>();
        final var possibleLocations = new PossiblePartialClassificationFinder().findPossibleLocationsOfPartialClassifications(projectStructureRoot, partitions);

        for (final var dimension : dimensionRegistry.getDimensions()) {
            if (partitions.stream().anyMatch(p -> p.getDimension().equals(dimension))) {
                continue;
            }

            var forcedValue = (Dimension.Partition) null;
            var sawAny = false;
            var ambiguous = false;

            for (final var possibleLocation : possibleLocations) {
                final var resolved = resolveAtLocation(possibleLocation, dimension);
                if (resolved instanceof Ambiguous) {
                    ambiguous = true;
                    break;
                }

                final var value = ((Fixed) resolved).partition();
                if (!sawAny) {
                    forcedValue = value;
                    sawAny = true;
                } else if (!Objects.equals(forcedValue, value)) {
                    ambiguous = true;
                    break;
                }
            }

            if (!ambiguous && sawAny) {
                result.put(dimension, forcedValue);
            }
        }
        return result;
    }

    /**
     * Resolves the value dimension takes on the ancestor path from the root down to location.
     */
    private LocationValue resolveAtLocation(final Modularity location, final Dimension dimension) {
        var current = location;
        var child = (Modularity) null;

        while (current != null) {
            if (current.getDimension() == dimension) {
                if (child == null) {
                    return new Ambiguous();
                }
                final var casePartitions = child.getCasePartitions();
                if (casePartitions == null || casePartitions.getPartitions().size() != 1) {
                    return new Ambiguous();
                }
                return new Fixed(casePartitions.getPartitions().iterator().next());
            }
            child = current;
            current = current.getParent().orElse(null);
        }

        return new Fixed(null);
    }

    /**
     * A dimension's value at one location: pinned to a partition (Fixed, null meaning never classified), or Ambiguous.
     */
    private sealed interface LocationValue {
    }

    private record Fixed(Dimension.Partition partition) implements LocationValue {
    }

    private record Ambiguous() implements LocationValue {
    }
}
