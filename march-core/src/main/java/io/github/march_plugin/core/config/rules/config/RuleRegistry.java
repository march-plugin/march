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

    private RuleRegistry(final List<Rule> rules, final RuleStrategy ruleStrategy) {
        this.rules = Collections.unmodifiableList(rules);
        this.ruleStrategy = ruleStrategy;
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

    public static class Builder {
        private final List<Rule> rules = new ArrayList<>();
        private RuleStrategy ruleStrategy;

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
         * Builds the rule registry.
         *
         * @return the built registry
         */
        public RuleRegistry build() {
            return new RuleRegistry(rules, ruleStrategy);
        }
    }
}