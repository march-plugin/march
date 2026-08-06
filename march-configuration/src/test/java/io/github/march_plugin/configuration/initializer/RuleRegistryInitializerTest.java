package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.rules.RuleConfigurationDto;
import io.github.march_plugin.configuration.dto.rules.RuleDto;
import io.github.march_plugin.configuration.dto.rules.RuleStrategyDto;
import io.github.march_plugin.configuration.dto.rules.RulesDto;
import io.github.march_plugin.configuration.dto.rules.ValidationScopeDto;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
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
        return new RuleConfigurationDto(strategy);
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
        final var registry = initializer.build(new RulesDto(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY)));

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_DENY);
    }

    @Test
    void shouldMapDefaultAllowStrategy() {
        final var registry = initializer.build(new RulesDto(List.of(), configOf(RuleStrategyDto.DEFAULT_ALLOW)));

        assertThat(registry.getRuleStrategy()).isEqualTo(RuleStrategy.DEFAULT_ALLOW);
    }

    @Test
    void shouldReturnEmptyRegistryWhenNoRulesGiven() {
        final var registry = initializer.build(new RulesDto(List.of(), configOf(RuleStrategyDto.DEFAULT_DENY)));

        assertThat(registry.getRules()).isEmpty();
    }

    @Test
    void shouldCompileRuleDefinitionAndRegisterRule() {
        final var ast = sampleAst();
        when(compiler.compile("source.layer == layer.service")).thenReturn(ast);

        final var rulesDto = new RulesDto(
                List.of(ruleDto("must be service", "source.layer == layer.service", null)),
                configOf(RuleStrategyDto.DEFAULT_DENY));

        final var registry = initializer.build(rulesDto);

        assertThat(registry.getRules()).hasSize(1);
        final var rule = registry.getRules().getFirst();
        assertThat(rule.description()).isEqualTo("must be service");
        assertThat(rule.definition()).isEqualTo(ast);
    }

    @Test
    void shouldDefaultToGlobalScopeWhenScopeIsNull() {
        when(compiler.compile(any())).thenReturn(sampleAst());

        final var rulesDto = new RulesDto(List.of(ruleDto("desc", "def", null)), configOf(RuleStrategyDto.DEFAULT_DENY));
        final var registry = initializer.build(rulesDto);

        assertThat(registry.getRules().getFirst().ruleScope()).isEqualTo(Rule.RuleScope.GLOBAL);
    }

    @ParameterizedTest
    @EnumSource(ValidationScopeDto.class)
    void shouldMapEveryScopeToMatchingRuleScope(final ValidationScopeDto scopeDto) {
        when(compiler.compile(any())).thenReturn(sampleAst());

        final var rulesDto = new RulesDto(List.of(ruleDto("desc", "def", scopeDto)), configOf(RuleStrategyDto.DEFAULT_DENY));
        final var registry = initializer.build(rulesDto);

        assertThat(registry.getRules().getFirst().ruleScope()).isEqualTo(scopeDto.toRuleScope());
    }

    @Test
    void shouldRegisterMultipleRulesInDeclarationOrder() {
        when(compiler.compile("d1")).thenReturn(sampleAst());
        when(compiler.compile("d2")).thenReturn(sampleAst());

        final var rulesDto = new RulesDto(
                List.of(ruleDto("first", "d1", null), ruleDto("second", "d2", null)),
                configOf(RuleStrategyDto.DEFAULT_ALLOW));

        final var registry = initializer.build(rulesDto);

        assertThat(registry.getRules()).extracting(Rule::description).containsExactly("first", "second");
    }

    @Test
    void shouldThrowWhenRulesListIsNull() {
        final var rulesDto = new RulesDto(null, configOf(RuleStrategyDto.DEFAULT_DENY));

        assertThatThrownBy(() -> initializer.build(rulesDto))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowWhenConfigurationIsNull() {
        final var rulesDto = new RulesDto(List.of(), null);

        assertThatThrownBy(() -> initializer.build(rulesDto))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowWhenStrategyIsNull() {
        final var rulesDto = new RulesDto(List.of(), configOf(null));

        assertThatThrownBy(() -> initializer.build(rulesDto))
                .isInstanceOf(NullPointerException.class);
    }
}
