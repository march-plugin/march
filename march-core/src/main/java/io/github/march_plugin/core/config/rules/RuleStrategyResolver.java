package io.github.march_plugin.core.config.rules;

import io.github.march_plugin.core.config.rules.config.RuleStrategy;
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
}
