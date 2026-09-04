package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;

import java.util.Set;

public class DefaultDenyDependencyPermissionEvaluator extends DependencyPermissionEvaluator {

    /**
     * Constructs the evaluator.
     *
     * @param projectStructureRoot the root of the project structure tree
     */
    public DefaultDenyDependencyPermissionEvaluator(final ModuleModularity projectStructureRoot) {
        super(projectStructureRoot);
    }

    @Override
    public DependencyPermission reduce(final RuleRegistry ruleRegistry, final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target) {
        final var partialMatch = evaluateRules(ruleRegistry, dimensionRegistry, source, target).stream()
                .filter(r -> !(r instanceof EvaluatedLogicalExpression.AlwaysFalse)).toList();

        if (partialMatch.stream().anyMatch(x -> x instanceof EvaluatedLogicalExpression.AlwaysTrue)) {
            return new DependencyPermission.Allowed();
        }

        if (!partialMatch.isEmpty()) {
            return new DependencyPermission.PartiallyAllowed(partialMatch);
        }

        return new DependencyPermission.Forbidden();
    }
}
