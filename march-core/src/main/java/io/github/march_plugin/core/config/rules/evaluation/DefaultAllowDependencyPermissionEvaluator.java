package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;

import java.util.Set;

public class DefaultAllowDependencyPermissionEvaluator extends DependencyPermissionEvaluator {

    /**
     * Constructs the evaluator.
     *
     * @param projectStructureRoot the root of the project structure tree
     */
    public DefaultAllowDependencyPermissionEvaluator(final ModuleModularity projectStructureRoot) {
        super(projectStructureRoot);
    }

    @Override
    public DependencyPermission reduce(final RuleRegistry ruleRegistry, final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target) {
        final var reducer = new RuleReducer();
        final var sourceNullDimensions = getAlwaysNullDimensions(dimensionRegistry, source);
        final var targetNullDimensions = getAlwaysNullDimensions(dimensionRegistry, target);

        final var evaluations = ruleRegistry.getRules().stream()
                .map(r -> reducer.reduce(r.definition(), source, target, sourceNullDimensions, targetNullDimensions))
                .toList();

        if (evaluations.stream().anyMatch(x -> x instanceof EvaluatedLogicalExpression.AlwaysTrue)) {
            return new DependencyPermission.Forbidden();
        }
        final var partialMatch = evaluations.stream()
                .filter(x -> !(x instanceof EvaluatedLogicalExpression.AlwaysFalse))
                .toList();

        if (!partialMatch.isEmpty()) {
            return new DependencyPermission.PartiallyAllowed(partialMatch);
        }

        return new DependencyPermission.Allowed();
    }
}
