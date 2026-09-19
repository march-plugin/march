package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleSetEquivalenceCheckerTest {

    private final RuleSetEquivalenceChecker checker = new RuleSetEquivalenceChecker();

    @Test
    void aPackageOnlyRuleOnlyAgreesWithAnEmptyRuleSetAtModuleLevelUnderAutomatic() {
        final var rulesA = List.<Rule>of();
        final var rulesB = List.of(new Rule("always matches", new LogicalExpression.AlwaysTrue(), Rule.RuleScope.PACKAGE_ONLY));

        final var underAutomatic = checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.AUTOMATIC, ScopeStrategy.AUTOMATIC, DependencyConfig.ANY_LEVEL);
        assertThat(underAutomatic).isEmpty();

        final var underManual = checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.MANUAL, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(underManual).isPresent();
        assertThat(underManual.get().context()).isEqualTo(Rule.RuleScope.MODULE_ONLY);
        assertThat(underManual.get().kind()).isEqualTo(RuleSetEquivalenceChecker.Disagreement.Kind.NEITHER_MATCHES);
    }

    @Test
    void mixedScopeStrategiesStillRecognizeEquivalentFormulations() {
        final var layerBuilder = new Dimension.Builder("layer");
        final var servicePart = layerBuilder.addPartition("service");
        layerBuilder.addPartition("ui");
        final var layerDim = layerBuilder.build();

        final var isService = definiteEquals(layerDim, servicePart);
        final var isNotService = definiteNotEquals(layerDim, servicePart);

        final var rulesA = List.of(
                new Rule("forbid service (module)", isService, Rule.RuleScope.MODULE_ONLY),
                new Rule("forbid service (package)", isService, Rule.RuleScope.PACKAGE_ONLY)
        );
        final var rulesB = List.of(
                new Rule("allow everything but service", isNotService, Rule.RuleScope.PACKAGE_ONLY)
        );

        final var mixedStrategies = checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.MANUAL, ScopeStrategy.AUTOMATIC, DependencyConfig.ANY_LEVEL);
        assertThat(mixedStrategies).isEmpty();

        final var wronglyAssumedManual = checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.MANUAL, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(wronglyAssumedManual).isPresent();
        assertThat(wronglyAssumedManual.get().context()).isEqualTo(Rule.RuleScope.MODULE_ONLY);
    }

    @Test
    void scopeStrategyIsIrrelevantWhenNoRuleIsPackageOnly() {
        final var layerBuilder = new Dimension.Builder("layer");
        final var servicePart = layerBuilder.addPartition("service");
        layerBuilder.addPartition("ui");
        final var layerDim = layerBuilder.build();

        final var isService = definiteEquals(layerDim, servicePart);
        final var isNotService = definiteNotEquals(layerDim, servicePart);

        final var rulesA = List.of(new Rule("forbid service", isService, Rule.RuleScope.GLOBAL));
        final var rulesB = List.of(new Rule("allow everything but service", isNotService, Rule.RuleScope.GLOBAL));

        assertThat(checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.AUTOMATIC, ScopeStrategy.AUTOMATIC, DependencyConfig.ANY_LEVEL)).isEmpty();
        assertThat(checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.MANUAL, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL)).isEmpty();
        assertThat(checker.findDisagreement(rulesA, rulesB, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_DENY, ScopeStrategy.AUTOMATIC, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL)).isEmpty();
    }

    @Test
    void samePolarityRuleSetsRequireMatchingExactlyTheSameClassifications() {
        final var layerBuilder = new Dimension.Builder("layer");
        final var servicePart = layerBuilder.addPartition("service");
        final var uiPart = layerBuilder.addPartition("ui");
        final var layerDim = layerBuilder.build();

        final var forbidService = comparisonWrap(new ComparisonExpression.Equal(sourceOf(layerDim), new PartitionExpression.Fixed(servicePart)));
        final var forbidServiceRestated = comparisonWrap(new ComparisonExpression.In(sourceOf(layerDim), List.of(new PartitionExpression.Fixed(servicePart))));
        final var forbidUi = comparisonWrap(new ComparisonExpression.Equal(sourceOf(layerDim), new PartitionExpression.Fixed(uiPart)));

        final var rulesA = List.of(new Rule("forbid service", forbidService, Rule.RuleScope.GLOBAL));
        final var rulesBEquivalent = List.of(new Rule("forbid service, restated", forbidServiceRestated, Rule.RuleScope.GLOBAL));
        final var rulesBDifferentPolicy = List.of(new Rule("forbid ui", forbidUi, Rule.RuleScope.GLOBAL));

        assertThat(checker.findDisagreement(rulesA, rulesBEquivalent, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_ALLOW, ScopeStrategy.MANUAL, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL)).isEmpty();

        final var disagreement = checker.findDisagreement(rulesA, rulesBDifferentPolicy, null, RuleStrategy.DEFAULT_ALLOW, RuleStrategy.DEFAULT_ALLOW, ScopeStrategy.MANUAL, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(disagreement).isPresent();
        assertThat(disagreement.get().kind()).isIn(RuleSetEquivalenceChecker.Disagreement.Kind.ONLY_A_MATCHES, RuleSetEquivalenceChecker.Disagreement.Kind.ONLY_B_MATCHES);
    }

    private static LogicalExpression definiteEquals(final Dimension dimension, final Dimension.Partition partition) {
        return new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.NotEqual(sourceOf(dimension), new PartitionExpression.Null())),
                comparisonWrap(new ComparisonExpression.Equal(sourceOf(dimension), new PartitionExpression.Fixed(partition)))
        );
    }

    private static LogicalExpression definiteNotEquals(final Dimension dimension, final Dimension.Partition partition) {
        return new LogicalExpression.Or(
                comparisonWrap(new ComparisonExpression.Equal(sourceOf(dimension), new PartitionExpression.Null())),
                new LogicalExpression.And(
                        comparisonWrap(new ComparisonExpression.NotEqual(sourceOf(dimension), new PartitionExpression.Null())),
                        comparisonWrap(new ComparisonExpression.NotEqual(sourceOf(dimension), new PartitionExpression.Fixed(partition)))
                )
        );
    }

    private static LogicalExpression.ComparisonWrap comparisonWrap(final ComparisonExpression comparison) {
        return new LogicalExpression.ComparisonWrap(comparison);
    }

    private static PartitionExpression.Relative sourceOf(final Dimension dimension) {
        return new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, dimension);
    }
}
