package io.github.march_plugin.core.rules;

import io.github.march_plugin.core.rules.config.RuleStrategy;
import io.github.march_plugin.core.rules.enforcement.DefaultAllowRuleEnforcer;
import io.github.march_plugin.core.rules.enforcement.DefaultDenyRuleEnforcer;
import io.github.march_plugin.core.rules.enforcement.PackageDependencyEvaluator;
import io.github.march_plugin.core.rules.enforcement.RuleEnforcer;
import io.github.march_plugin.core.rules.evaluation.RuleEvaluator;

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
     * @param ruleEvaluator evaluates if a dependency matches a rule
     * @return the rule enforcer
     */
    public RuleEnforcer getRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final RuleEvaluator ruleEvaluator) {
        return switch (ruleStrategy) {
            case DEFAULT_DENY -> new DefaultDenyRuleEnforcer(packageDependencyEvaluator, ruleEvaluator);
            case DEFAULT_ALLOW -> new DefaultAllowRuleEnforcer(packageDependencyEvaluator, ruleEvaluator);
        };
    }
}
