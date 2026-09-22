package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.MarchConfigDto;
import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;
import io.github.march_plugin.configuration.initializer.exception.UnresolvedActiveRuleSetException;
import io.github.march_plugin.configuration.initializer.exception.UnresolvedRuleSetException;
import io.github.march_plugin.core.config.rules.config.DependencyConfig;
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

    /**
     * Builds the rule registry for the named rule set, or the active rule set from settings if
     * {@code ruleSetName} is {@code null}.
     *
     * @param marchConfigDto the parsed March configuration
     * @param ruleSetName    the rule set to build
     * @return the rule set's registry
     * @throws UnresolvedActiveRuleSetException if {@code ruleSetName} is {@code null} and the active rule set cannot be resolved
     * @throws UnresolvedRuleSetException if {@code ruleSetName} is given but does not match any declared rule set
     */
    public RuleRegistry buildActive(final MarchConfigDto marchConfigDto, final String ruleSetName) {
        if (ruleSetName == null) {
            return buildActive(marchConfigDto);
        }

        final var declaredRuleSets = marchConfigDto.rules();
        final var selected = declaredRuleSets.stream()
                .filter(ruleSet -> ruleSetName.equals(ruleSet.name()))
                .findFirst()
                .orElseThrow(() -> new UnresolvedRuleSetException(ruleSetName, declaredRuleSets));

        return build(selected.rules(), selected.config());
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

        final var dependencyConfigDto = ruleConfigurationDto == null ? null : ruleConfigurationDto.dependencyConfig();
        ruleRegistryBuilder.setDependencyConfig(dependencyConfigDto == null ? DependencyConfig.LEAVES_ONLY : switch (dependencyConfigDto) {
            case ANY_LEVEL -> DependencyConfig.ANY_LEVEL;
            case LEAVES_ONLY -> DependencyConfig.LEAVES_ONLY;
        });
    }

    private void registerRules(final RuleRegistry.Builder ruleRegistryBuilder, final List<RuleDto> rules) {
        for (final var ruleDto : rules) {
            final var ast = ruleDefinitionCompiler.compile(ruleDto.definition());
            ruleRegistryBuilder.addRule(new Rule(ruleDto.description(), ast, ruleDto.scope() == null ? Rule.RuleScope.GLOBAL : ruleDto.scope().toRuleScope()));
        }
    }
}