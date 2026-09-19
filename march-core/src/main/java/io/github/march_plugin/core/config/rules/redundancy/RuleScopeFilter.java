package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;

import java.util.List;

/**
 * Filters rules in a given {@link Rule.RuleScope} context.
 */
final class RuleScopeFilter {

    private RuleScopeFilter() {
    }

    static List<Rule> effectiveRules(final List<Rule> rules, final Rule.RuleScope contextScope, final ScopeStrategy scopeStrategy) {
        return rules.stream()
                .filter(rule -> appliesInContext(rule, contextScope, scopeStrategy))
                .toList();
    }

    private static boolean appliesInContext(final Rule rule, final Rule.RuleScope contextScope, final ScopeStrategy scopeStrategy) {
        if (rule.ruleScope() == Rule.RuleScope.GLOBAL || rule.ruleScope() == contextScope) {
            return true;
        }
        return contextScope == Rule.RuleScope.MODULE_ONLY
                && scopeStrategy == ScopeStrategy.AUTOMATIC
                && rule.ruleScope() == Rule.RuleScope.PACKAGE_ONLY;
    }
}
