package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;
import io.github.march_plugin.configuration.dto.rules.RulesDto;
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
    private final RuleRegistry.Builder ruleRegistryBuilder = new RuleRegistry.Builder();

    /**
     * Constructs the initializer.
     *
     * @param ruleDefinitionCompiler the compiler used to compile rule definitions into their AST representation
     */
    public RuleRegistryInitializer(final RuleDefinitionCompiler ruleDefinitionCompiler) {
        this.ruleDefinitionCompiler = ruleDefinitionCompiler;
    }

    /**
     * Builds the rule registry from the given rule configuration.
     *
     * @param rulesDto the rules and rule strategy declared in the March configuration
     * @return the built rule registry
     */
    public RuleRegistry build(final RulesDto rulesDto) {
        registerRuleConfig(rulesDto.configuration());
        registerRules(rulesDto.rules());
        return ruleRegistryBuilder.build();
    }

    private void registerRuleConfig(final RuleConfigurationDto ruleConfigurationDto) {
        final var ruleStrategy = switch (ruleConfigurationDto.ruleStrategy()) {
            case DEFAULT_DENY -> RuleStrategy.DEFAULT_DENY;
            case DEFAULT_ALLOW -> RuleStrategy.DEFAULT_ALLOW;
        };
        ruleRegistryBuilder.setRuleStrategy(ruleStrategy);

        final var scopeStrategyDto = ruleConfigurationDto.scopeStrategy();
        if (scopeStrategyDto != null) {
            ruleRegistryBuilder.setScopeStrategy(switch (scopeStrategyDto) {
                case AUTOMATIC -> ScopeStrategy.AUTOMATIC;
                case MANUAL -> ScopeStrategy.MANUAL;
            });
        }
    }

    private void registerRules(final List<RuleDto> rules) {
        for (final var ruleDto : rules) {
            final var ast = ruleDefinitionCompiler.compile(ruleDto.definition());
            ruleRegistryBuilder.addRule(new Rule(ruleDto.description(), ast, ruleDto.scope() == null ? Rule.RuleScope.GLOBAL : ruleDto.scope().toRuleScope()));
        }
    }
}