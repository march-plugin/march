package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluationResult;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.config.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.enforcement.rules.exceptions.DependencyNotAllowedException;
import io.github.march_plugin.core.enforcement.rules.exceptions.PackageDependencyNotAllowedException;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.project.ProjectModuleRegistry;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDenyRuleEnforcerTest {

    private PackageDependencyEvaluator packageDependencyEvaluator;

    private final Dimension.Builder domainBuilder = new Dimension.Builder("domain");
    private final Dimension.Partition domainA = domainBuilder.addPartition("a");
    private final Dimension.Partition domainB = domainBuilder.addPartition("b");
    private final Dimension domain = domainBuilder.build();

    private final Dimension.Builder layerBuilder = new Dimension.Builder("layer");
    private final Dimension.Partition layerBusiness = layerBuilder.addPartition("business");
    private final Dimension.Partition layerDbaccess = layerBuilder.addPartition("dbaccess");
    private final Dimension layer = layerBuilder.build();

    private final LogicalExpression sameDomainRule = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.Equal(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, domain),
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, domain)));

    private final LogicalExpression businessToDbaccessRule = new LogicalExpression.And(
            sameDomainRule,
            new LogicalExpression.And(
                    new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                            new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layer),
                            new PartitionExpression.Fixed(layerBusiness))),
                    new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                            new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, layer),
                            new PartitionExpression.Fixed(layerDbaccess)))));

    private final LogicalExpression differentDomainRule = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.NotEqual(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, domain),
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, domain)));

    private final LogicalExpression sameDomainAndBusinessLayerRule = new LogicalExpression.And(
            sameDomainRule,
            new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layer),
                    new PartitionExpression.Fixed(layerBusiness))));

    @BeforeEach
    void setUpSharedFixtures() {
        packageDependencyEvaluator = mock(PackageDependencyEvaluator.class);
    }

    @Nested
    class AutomaticMode {

        private RuleEvaluator ruleEvaluator;
        private DefaultDenyRuleEnforcer enforcer;

        @BeforeEach
        void setUp() {
            ruleEvaluator = mock(RuleEvaluator.class);
            enforcer = new TestableDefaultDenyRuleEnforcer(packageDependencyEvaluator, ruleEvaluator, ScopeStrategy.AUTOMATIC);
        }

        @Test
        void enforceMavenAllowedWhenRuleMatchesDirectlyAtModuleLevel() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain", sameDomainRule, null);

            when(ruleEvaluator.evaluate(sameDomainRule, source, target)).thenReturn(true);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenSkipsRuleReferencingDimensionMissingFromModuleClassification() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Business to dbaccess", businessToDbaccessRule, null);

            assertThrows(DependencyNotAllowedException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));

            verify(ruleEvaluator, never()).evaluate(eq(businessToDbaccessRule), eq(source), eq(target));
        }

        @Test
        void enforceMavenViolationWhenNoRuleMatchesAndNoPackagesExist() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain", sameDomainRule, null);

            when(ruleEvaluator.evaluate(sameDomainRule, source, target)).thenReturn(false);

            assertThrows(DependencyNotAllowedException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenAllowedViaPackageFallbackWhenModuleCheckFindsNothing() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Business to dbaccess", businessToDbaccessRule, null);

            final var businessPackage = mockPackage("source.business", domainA, layerBusiness);
            final var dbaccessPackage = mockPackage("target.dbaccess", domainA, layerDbaccess);

            when(ruleEvaluator.evaluate(businessToDbaccessRule, businessPackage.classification(), dbaccessPackage.classification())).thenReturn(true);
            when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(new PackageDependencyEvaluationResult(false, ""));

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(businessPackage, dbaccessPackage), List.of(rule)));
        }

        @Test
        void enforceMavenViolationWhenFallbackFindsNoAllowedPackagePairEither() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Business to dbaccess", businessToDbaccessRule, null);

            final var businessPackage = mockPackage("source.business", domainA, layerBusiness);
            final var dbaccessPackage = mockPackage("target.dbaccess", domainA, layerDbaccess);

            when(ruleEvaluator.evaluate(businessToDbaccessRule, businessPackage.classification(), dbaccessPackage.classification())).thenReturn(false);

            assertThrows(DependencyNotAllowedException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(businessPackage, dbaccessPackage), List.of(rule)));
        }

        @Test
        void enforceMavenAllowedWhenSecondRuleMatchesAfterFirstDoesNotMatch() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var nonMatchingRule = new Rule("Same domain", sameDomainRule, null);
            final var matchingRule = new Rule("Different domain", differentDomainRule, null);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(nonMatchingRule, matchingRule)));
        }

        @Test
        void enforceMavenViolationWhenCompositeRuleShortCircuitsOnResolvedFalseBranch() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain and business", sameDomainAndBusinessLayerRule, null);

            assertThrows(DependencyNotAllowedException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }
    }

    @Nested
    class ManualMode {

        private RuleEvaluator ruleEvaluator;
        private DefaultDenyRuleEnforcer enforcer;

        @BeforeEach
        void setUp() {
            ruleEvaluator = mock(RuleEvaluator.class);
            enforcer = new TestableDefaultDenyRuleEnforcer(packageDependencyEvaluator, ruleEvaluator, ScopeStrategy.MANUAL);
        }

        @Test
        void enforceMavenNeverAttemptsPackageFallback() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Business to dbaccess", businessToDbaccessRule, Rule.RuleScope.PACKAGE_ONLY);

            final var businessPackage = mockPackage("source.business", domainA, layerBusiness);
            final var dbaccessPackage = mockPackage("target.dbaccess", domainA, layerDbaccess);

            when(ruleEvaluator.evaluate(businessToDbaccessRule, businessPackage.classification(), dbaccessPackage.classification())).thenReturn(true);

            assertThrows(DependencyNotAllowedException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(businessPackage, dbaccessPackage), List.of(rule)));
        }

        @Test
        void enforceMavenAllowedWhenRuleMatchesConcretely() {
            final var realEnforcer = new DefaultDenyRuleEnforcer(packageDependencyEvaluator, ScopeStrategy.MANUAL);
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.GLOBAL);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(realEnforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenViolationWhenRuleDoesNotMatch() {
            final var realEnforcer = new DefaultDenyRuleEnforcer(packageDependencyEvaluator, ScopeStrategy.MANUAL);
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.GLOBAL);

            assertThrows(DependencyNotAllowedException.class, () ->
                    invokeEnforceRules(realEnforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }
    }

    @Nested
    class PackageLevel {

        private RuleEvaluator ruleEvaluator;
        private DefaultDenyRuleEnforcer enforcer;

        @BeforeEach
        void setUp() {
            ruleEvaluator = mock(RuleEvaluator.class);
            enforcer = new TestableDefaultDenyRuleEnforcer(packageDependencyEvaluator, ruleEvaluator, ScopeStrategy.AUTOMATIC);
        }

        @Test
        void enforcePackageViolationMessage() {
            final var source = mockPackage("io.source", domainA);
            final var target = mockPackage("io.target", domainA);
            final var rules = List.<Rule>of();

            final var result = new PackageDependencyEvaluationResult(true, "FORBIDDEN BY DEFAULT");
            when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(result);

            final var ex = assertThrows(PackageDependencyNotAllowedException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(), List.of(source, target), rules));

            assertThat(ex.getMessage()).contains("io.source -> io.target");
            assertThat(ex.getMessage()).contains("FORBIDDEN BY DEFAULT");
        }

        @Test
        void enforcePackageAllowedByMatchingRule() {
            final var source = mockPackage("io.source", domainA);
            final var target = mockPackage("io.target", domainA);
            final var rule = new Rule("Allow Pkg", sameDomainRule, Rule.RuleScope.PACKAGE_ONLY);

            final var result = new PackageDependencyEvaluationResult(true, "Actual code dependency exists");
            when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(result);

            when(ruleEvaluator.evaluate(any(), any(), any())).thenReturn(true);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(rule)));
        }

        @Test
        void enforcePackageAllowedWhenNoRealDependencyExists() {
            final var source = mockPackage("io.source", domainA);
            final var target = mockPackage("io.target", domainA);
            final var rules = List.<Rule>of();

            when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(new PackageDependencyEvaluationResult(false, ""));

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(), List.of(source, target), rules));
        }
    }

    private static Classification mockClassification(final Dimension.Partition... partitions) {
        final var classification = mock(Classification.class);
        when(classification.getPartitions()).thenReturn(Set.of(partitions));
        return classification;
    }

    private static PackageClassification mockPackage(final String path, final Dimension.Partition... partitions) {
        final var classification = mockClassification(partitions);
        final var hierarchy = new PackageHierarchy(List.of(path.split("\\.")));
        return new PackageClassification(classification, hierarchy, true);
    }

    private static void invokeEnforceRules(final RuleEnforcer enforcer, final Set<MavenDependency> dependencies,
                                            final Collection<PackageClassification> packages, final List<Rule> rules) {
        final var classificationRegistry = mock(ClassificationRegistry.class);
        final var projectModuleRegistry = mock(ProjectModuleRegistry.class);
        final var ruleRegistry = mock(RuleRegistry.class);

        final var classifiedPackages = packages.stream().map(p -> {
            final var classifiedPackage = mock(ClassifiedPackage.class);
            when(classifiedPackage.getClassifiedPackage()).thenReturn(p);
            return classifiedPackage;
        }).toList();

        when(projectModuleRegistry.getDependencies(classificationRegistry)).thenReturn(dependencies);
        when(classificationRegistry.getAllClassifiedPackages()).thenReturn(classifiedPackages);
        when(ruleRegistry.getRules()).thenReturn(rules);

        enforcer.enforceRules(classificationRegistry, projectModuleRegistry, ruleRegistry);
    }

    private static class TestableDefaultDenyRuleEnforcer extends DefaultDenyRuleEnforcer {
        private final RuleEvaluator ruleEvaluator;

        TestableDefaultDenyRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final RuleEvaluator ruleEvaluator, final ScopeStrategy scopeStrategy) {
            super(packageDependencyEvaluator, scopeStrategy);
            this.ruleEvaluator = ruleEvaluator;
        }

        @Override
        protected RuleEvaluator getRuleEvaluator() {
            return ruleEvaluator;
        }
    }
}
