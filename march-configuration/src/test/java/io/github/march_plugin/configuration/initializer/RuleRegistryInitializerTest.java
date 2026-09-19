package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.MarchConfigDto;
import io.github.march_plugin.configuration.dto.SettingsDto;
import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;
import io.github.march_plugin.configuration.dto.rules.RuleSetDto;
import io.github.march_plugin.configuration.dto.rules.RuleStrategyDto;
import io.github.march_plugin.configuration.dto.rules.ScopeStrategyDto;
import io.github.march_plugin.configuration.dto.rules.ValidationScopeDto;
import io.github.march_plugin.configuration.initializer.exception.UnresolvedActiveRuleSetException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleRegistryInitializerTest {

    private final RuleDefinitionCompiler compiler = mock(RuleDefinitionCompiler.class);
    private final RuleRegistryInitializer initializer = new RuleRegistryInitializer(compiler);

    private static RuleConfigurationDto configOf(final RuleStrategyDto strategy) {
        return new RuleConfigurationDto(strategy, null, null);
    }

    private static RuleConfigurationDto configOf(final RuleStrategyDto strategy, final ScopeStrategyDto scopeStrategy) {
        return new RuleConfigurationDto(strategy, scopeStrategy, null);
    }

    private static RuleDto ruleDto(final String description, final String definition, final ValidationScopeDto scope) {
        return new RuleDto(description, definition, scope);
    }

    private static LogicalExpression sampleAst() {
        final var builder = new Dimension.Builder("layer");
        final var service = builder.addPartition("service");
        builder.addPartition("web");
        final var dimension = builder.build();

        return new LogicalExpression.ComparisonWrap(
                new ComparisonExpression.Equal(
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, dimension),
                        new PartitionExpression.Fixed(service)));
    }

    @Test
    void shouldMapDefaultDenyStrategy() {
        final var registry = initializer.build(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
    }

    @Test
    void shouldMapDefaultAllowStrategy() {
        final var registry = initializer.build(List.of(), configOf(RuleStrategyDto.DEFAULT_ALLOW));

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_ALLOW);
    }

    @Test
    void shouldDefaultToManualScopeStrategyWhenNotConfigured() {
        final var registry = initializer.build(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getScopeStrategy()).isEqualTo(ScopeStrategy.MANUAL);
    }

    @Test
    void shouldMapAutomaticScopeStrategy() {
        final var registry = initializer.build(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY, ScopeStrategyDto.AUTOMATIC));

        assertThat(registry.getScopeStrategy()).isEqualTo(ScopeStrategy.AUTOMATIC);
    }

    @Test
    void shouldMapManualScopeStrategy() {
        final var registry = initializer.build(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY, ScopeStrategyDto.MANUAL));

        assertThat(registry.getScopeStrategy()).isEqualTo(ScopeStrategy.MANUAL);
    }

    @Test
    void shouldReturnEmptyRegistryWhenNoRulesGiven() {
        final var registry = initializer.build(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getRules()).isEmpty();
    }

    @Test
    void shouldCompileRuleDefinitionAndRegisterRule() {
        final var ast = sampleAst();
        when(compiler.compile("source.layer == layer.service")).thenReturn(ast);

        final var rules = List.of(ruleDto("must be service", "source.layer == layer.service", null));

        final var registry = initializer.build(rules, configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getRules()).hasSize(1);
        final var rule = registry.getRules().getFirst();
        assertThat(rule.description()).isEqualTo("must be service");
        assertThat(rule.definition()).isEqualTo(ast);
    }

    @Test
    void shouldDefaultToGlobalScopeWhenScopeIsNull() {
        when(compiler.compile(any())).thenReturn(sampleAst());

        final var rules = List.of(ruleDto("desc", "def", null));
        final var registry = initializer.build(rules, configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getRules().getFirst().ruleScope()).isEqualTo(Rule.RuleScope.GLOBAL);
    }

    @ParameterizedTest
    @EnumSource(ValidationScopeDto.class)
    void shouldMapEveryScopeToMatchingRuleScope(final ValidationScopeDto scopeDto) {
        when(compiler.compile(any())).thenReturn(sampleAst());

        final var rules = List.of(ruleDto("desc", "def", scopeDto));
        final var registry = initializer.build(rules, configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getRules().getFirst().ruleScope()).isEqualTo(scopeDto.toRuleScope());
    }

    @Test
    void shouldRegisterMultipleRulesInDeclarationOrder() {
        when(compiler.compile("d1")).thenReturn(sampleAst());
        when(compiler.compile("d2")).thenReturn(sampleAst());

        final var rules = List.of(ruleDto("first", "d1", null), ruleDto("second", "d2", null));

        final var registry = initializer.build(rules, configOf(RuleStrategyDto.DEFAULT_ALLOW));

        assertThat(registry.getRules()).extracting(Rule::description).containsExactly("first", "second");
    }

    @Test
    void shouldNotLeakRulesBetweenRepeatedBuildCallsOnTheSameInitializer() {
        when(compiler.compile("d1")).thenReturn(sampleAst());
        when(compiler.compile("d2")).thenReturn(sampleAst());

        // Building two registries from one initializer instance is exactly what comparing two rule sets for
        // equivalence needs to do; the second call must not see the first call's rules or vice versa.
        final var registryA = initializer.build(List.of(ruleDto("a", "d1", null)), configOf(RuleStrategyDto.DEFAULT_ALLOW));
        final var registryB = initializer.build(List.of(ruleDto("b", "d2", null)), configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registryA.getRules()).extracting(Rule::description).containsExactly("a");
        assertThat(registryB.getRules()).extracting(Rule::description).containsExactly("b");
        assertThat(registryA.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_ALLOW);
        assertThat(registryB.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
    }

    @Test
    void shouldReturnEmptyRegistryWhenRulesListIsNull() {
        final var registry = initializer.build(null, configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThat(registry.getRules()).isEmpty();
    }

    @Test
    void shouldDefaultToDenyAndManualWhenConfigurationIsNull() {
        final var registry = initializer.build(List.of(), null);

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
        assertThat(registry.getScopeStrategy()).isEqualTo(ScopeStrategy.MANUAL);
    }

    @Test
    void shouldDefaultToDenyWhenStrategyIsNull() {
        final var registry = initializer.build(List.of(), configOf(null));

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
    }

    @Test
    void buildActiveShouldReturnEmptyDefaultRegistryWhenNoRuleSetsDeclared() {
        final var marchConfigDto = new MarchConfigDto(null, null, null, null, null, List.of());

        final var registry = initializer.buildActive(marchConfigDto);

        assertThat(registry.getRules()).isEmpty();
        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
        assertThat(registry.getScopeStrategy()).isEqualTo(ScopeStrategy.MANUAL);
    }

    @Test
    void buildActiveShouldResolveAndBuildTheNamedActiveRuleSet() {
        when(compiler.compile(any())).thenReturn(sampleAst());

        final var allow = new RuleSetDto("allow", configOf(RuleStrategyDto.DEFAULT_ALLOW), List.of(ruleDto("a", "d", null)));
        final var deny = new RuleSetDto("deny", configOf(RuleStrategyDto.DEFAULT_DENY), List.of());
        final var settings = new SettingsDto("deny", null, null, null, null, null);
        final var marchConfigDto = new MarchConfigDto(null, null, null, null, settings, List.of(allow, deny));

        final var registry = initializer.buildActive(marchConfigDto);

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
        assertThat(registry.getRules()).isEmpty();
    }

    @Test
    void buildActiveShouldThrowWhenActiveRuleSetIsMissingButRuleSetsAreDeclared() {
        final var allow = new RuleSetDto("allow", configOf(RuleStrategyDto.DEFAULT_ALLOW), List.of());
        final var marchConfigDto = new MarchConfigDto(null, null, null, null, null, List.of(allow));

        assertThatThrownBy(() -> initializer.buildActive(marchConfigDto))
                .isInstanceOf(UnresolvedActiveRuleSetException.class);
    }

    @Test
    void buildActiveShouldThrowWhenActiveRuleSetNameDoesNotMatchAnyDeclaredRuleSet() {
        final var allow = new RuleSetDto("allow", configOf(RuleStrategyDto.DEFAULT_ALLOW), List.of());
        final var settings = new SettingsDto("typo", null, null, null, null, null);
        final var marchConfigDto = new MarchConfigDto(null, null, null, null, settings, List.of(allow));

        assertThatThrownBy(() -> initializer.buildActive(marchConfigDto))
                .isInstanceOf(UnresolvedActiveRuleSetException.class);
    }
}
