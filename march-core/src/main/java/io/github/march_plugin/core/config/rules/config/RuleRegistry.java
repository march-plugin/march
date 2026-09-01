package io.github.march_plugin.core.config.rules.config;

import io.github.march_plugin.core.config.rules.model.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of the dimensions and partitions defined in the configuration.
 */
public final class RuleRegistry {

    private final List<Rule> rules;
    private final RuleStrategy ruleStrategy;
    private final ScopeStrategy scopeStrategy;

    private RuleRegistry(final List<Rule> rules, final RuleStrategy ruleStrategy, final ScopeStrategy scopeStrategy) {
        this.rules = Collections.unmodifiableList(rules);
        this.ruleStrategy = ruleStrategy;
        this.scopeStrategy = scopeStrategy;
    }

    /**
     * Gets the rules.
     *
     * @return the rules
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Gets the rule strategy.
     *
     * @return the rule strategy
     */
    public RuleStrategy getRuleStrategy() {
        return ruleStrategy;
    }

    /**
     * Gets the scope strategy.
     *
     * @return the scope strategy
     */
    public ScopeStrategy getScopeStrategy() {
        return scopeStrategy;
    }

    public static class Builder {
        private final List<Rule> rules = new ArrayList<>();
        private RuleStrategy ruleStrategy;
        private ScopeStrategy scopeStrategy = ScopeStrategy.AUTOMATIC;

        /**
         * Adds a rule to the registry.
         *
         * @param rule the rule to add to the registry
         */
        public void addRule(final Rule rule) {
            rules.add(rule);
        }

        /**
         * Sets the rule strategy.
         *
         * @param ruleStrategy the rule strategy
         */
        public void setRuleStrategy(final RuleStrategy ruleStrategy) {
            this.ruleStrategy = ruleStrategy;
        }

        /**
         * Sets the scope strategy.
         *
         * @param scopeStrategy the scope strategy
         */
        public void setScopeStrategy(final ScopeStrategy scopeStrategy) {
            this.scopeStrategy = scopeStrategy;
        }

        /**
         * Builds the rule registry.
         *
         * @return the built registry
         */
        public RuleRegistry build() {
            return new RuleRegistry(rules, ruleStrategy, scopeStrategy);
        }
    }
}