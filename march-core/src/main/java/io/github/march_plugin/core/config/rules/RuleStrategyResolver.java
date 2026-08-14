package io.github.march_plugin.core.config.rules;

import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.evaluation.DefaultAllowDependencyPermissionEvaluator;
import io.github.march_plugin.core.config.rules.evaluation.DefaultDenyDependencyPermissionEvaluator;
import io.github.march_plugin.core.config.rules.evaluation.DependencyPermissionEvaluator;
import io.github.march_plugin.core.enforcement.rules.DefaultAllowRuleEnforcer;
import io.github.march_plugin.core.enforcement.rules.DefaultDenyRuleEnforcer;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.rules.RuleEnforcer;

/**
 * Resolves the correct classes depending on configured {@link RuleStrategy}.
 */
public class RuleStrategyResolver {

    private final RuleStrategy ruleStrategy;

    /**
     * Builds the resolver.
     *
     * @param ruleStrategy the configured strategy
     */
    public RuleStrategyResolver(final RuleStrategy ruleStrategy) {
        this.ruleStrategy = ruleStrategy;

    }

    /**
     * Gets the rule enforcer.
     *
     * @param packageDependencyEvaluator evaluates forbidden package dependencies
     * @return the rule enforcer
     */
    public RuleEnforcer getRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator) {
        return switch (ruleStrategy) {
            case DEFAULT_DENY -> new DefaultDenyRuleEnforcer(packageDependencyEvaluator);
            case DEFAULT_ALLOW -> new DefaultAllowRuleEnforcer(packageDependencyEvaluator);
        };
    }

    /**
     * Gets the evaluator for partial dependencies.
     *
     * @param projectStructureRoot the root of the project structure tree, needed to determine which dimensions can structurally never apply to a given partial classification
     * @return the evaluator
     */
    public DependencyPermissionEvaluator getDependencyPermissionEvaluator(final ModuleModularity projectStructureRoot) {
        return switch (ruleStrategy) {
            case DEFAULT_DENY -> new DefaultDenyDependencyPermissionEvaluator(projectStructureRoot);
            case DEFAULT_ALLOW -> new DefaultAllowDependencyPermissionEvaluator(projectStructureRoot);
        };
    }
}
