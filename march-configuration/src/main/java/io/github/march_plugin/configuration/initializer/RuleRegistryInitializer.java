package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.MarchConfigDto;
import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;
import io.github.march_plugin.configuration.initializer.exception.UnresolvedActiveRuleSetException;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;

import java.util.List;

/**
 * Builds a {@link RuleRegistry} from the rules and rule strategy declared in the March configuration.
 */
public class RuleRegistryInitializer {

    private final RuleDefinitionCompiler ruleDefinitionCompiler;

    /**
     * Constructs the initializer.
     *
     * @param ruleDefinitionCompiler the compiler used to compile rule definitions into their AST representation
     */
    public RuleRegistryInitializer(final RuleDefinitionCompiler ruleDefinitionCompiler) {
        this.ruleDefinitionCompiler = ruleDefinitionCompiler;
    }

    /**
     * Builds the rule registry from the given rules and rule strategy configuration.
     *
     * @param rules the rules declared in March config
     * @param ruleConfigurationDto the rule strategy declared in March config
     * @return the built rule registry
     */
    public RuleRegistry build(final List<RuleDto> rules, final RuleConfigurationDto ruleConfigurationDto) {
        final var ruleRegistryBuilder = new RuleRegistry.Builder();
        registerRuleConfig(ruleRegistryBuilder, ruleConfigurationDto);
        registerRules(ruleRegistryBuilder, rules == null ? List.of() : rules);
        return ruleRegistryBuilder.build();
    }

    /**
     * Builds the rule registry for the rule set actually enforced at build time.
     *
     * @param marchConfigDto the parsed March configuration
     * @return the active rule set's registry
     * @throws UnresolvedActiveRuleSetException if ruleset cannot be resolved
     */
    public RuleRegistry buildActive(final MarchConfigDto marchConfigDto) {
        final var declaredRuleSets = marchConfigDto.rules();
        if (declaredRuleSets.isEmpty()) {
            return build(List.of(), null);
        }

        final var active = marchConfigDto.activeRuleSet();
        if (active == null) {
            final var activeRuleSetName = marchConfigDto.settings() == null ? null : marchConfigDto.settings().activeRuleSet();
            throw new UnresolvedActiveRuleSetException(activeRuleSetName, declaredRuleSets);
        }

        return build(active.rules(), active.config());
    }

    private void registerRuleConfig(final RuleRegistry.Builder ruleRegistryBuilder, final RuleConfigurationDto ruleConfigurationDto) {
        final var ruleStrategyDto = ruleConfigurationDto == null ? null : ruleConfigurationDto.ruleStrategy();
        ruleRegistryBuilder.setRuleStrategy(ruleStrategyDto == null ? RuleStrategy.DEFAULT_DENY : switch (ruleStrategyDto) {
            case DEFAULT_DENY -> RuleStrategy.DEFAULT_DENY;
            case DEFAULT_ALLOW -> RuleStrategy.DEFAULT_ALLOW;
        });

        final var scopeStrategyDto = ruleConfigurationDto == null ? null : ruleConfigurationDto.scopeStrategy();
        ruleRegistryBuilder.setScopeStrategy(scopeStrategyDto == null ? ScopeStrategy.MANUAL : switch (scopeStrategyDto) {
            case AUTOMATIC -> ScopeStrategy.AUTOMATIC;
            case MANUAL -> ScopeStrategy.MANUAL;
        });
    }

    private void registerRules(final RuleRegistry.Builder ruleRegistryBuilder, final List<RuleDto> rules) {
        for (final var ruleDto : rules) {
            final var ast = ruleDefinitionCompiler.compile(ruleDto.definition());
            ruleRegistryBuilder.addRule(new Rule(ruleDto.description(), ast, ruleDto.scope() == null ? Rule.RuleScope.GLOBAL : ruleDto.scope().toRuleScope()));
        }
    }
}