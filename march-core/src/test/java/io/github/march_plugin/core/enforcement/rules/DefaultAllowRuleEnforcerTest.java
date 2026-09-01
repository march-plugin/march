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
import io.github.march_plugin.core.enforcement.rules.exceptions.DependencyForbiddenException;
import io.github.march_plugin.core.enforcement.rules.exceptions.PackageDependencyForbiddenException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAllowRuleEnforcerTest {

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

    private final LogicalExpression notBusinessLayerRule = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.NotEqual(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layer),
                    new PartitionExpression.Fixed(layerBusiness)));

    private final LogicalExpression differentDomainRule = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.NotEqual(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, domain),
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, domain)));

    private final LogicalExpression sameDomainAndBusinessLayerRule = new LogicalExpression.And(
            sameDomainRule,
            new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layer),
                    new PartitionExpression.Fixed(layerBusiness))));

    private final LogicalExpression dummyExpression = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.Equal(new PartitionExpression.Null(), new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, null))
    );

    @BeforeEach
    void setUpSharedFixtures() {
        packageDependencyEvaluator = mock(PackageDependencyEvaluator.class);
    }

    @Nested
    class AutomaticMode {

        private RuleEvaluator ruleEvaluator;
        private DefaultAllowRuleEnforcer enforcer;

        @BeforeEach
        void setUp() {
            ruleEvaluator = mock(RuleEvaluator.class);
            enforcer = new TestableDefaultAllowRuleEnforcer(packageDependencyEvaluator, ruleEvaluator, ScopeStrategy.AUTOMATIC);
        }

        @Test
        void enforceMavenViolationWhenRuleMatchesConcretely() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Forbidden Dep", sameDomainRule, null);

            assertThrows(DependencyForbiddenException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenAllowedWhenRuleDoesNotMatch() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Forbidden Dep", sameDomainRule, null);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenCorrectlyHandlesMissingDimension() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Forbid non-business (misscoped)", notBusinessLayerRule, null);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenViolationWhenSecondRuleMatchesAfterFirstDoesNotMatch() {
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var nonMatchingRule = new Rule("Same domain", sameDomainRule, null);
            final var matchingRule = new Rule("Different domain", differentDomainRule, null);

            assertThrows(DependencyForbiddenException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(nonMatchingRule, matchingRule)));
        }
    }

    @Nested
    class ManualMode {

        private RuleEvaluator ruleEvaluator;
        private DefaultAllowRuleEnforcer enforcer;

        @BeforeEach
        void setUp() {
            ruleEvaluator = mock(RuleEvaluator.class);
            enforcer = new TestableDefaultAllowRuleEnforcer(packageDependencyEvaluator, ruleEvaluator, ScopeStrategy.MANUAL);
        }

        @Test
        void enforceMavenViolationWhenRuleMatchesConcretely() {
            final var realEnforcer = new DefaultAllowRuleEnforcer(packageDependencyEvaluator, ScopeStrategy.MANUAL);
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.GLOBAL);

            assertThrows(DependencyForbiddenException.class, () ->
                    invokeEnforceRules(realEnforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenAllowedWhenRuleDoesNotMatch() {
            final var realEnforcer = new DefaultAllowRuleEnforcer(packageDependencyEvaluator, ScopeStrategy.MANUAL);
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainB);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.GLOBAL);

            assertDoesNotThrow(() ->
                    invokeEnforceRules(realEnforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }

        @Test
        void enforceMavenNeverEvaluatesPackageOnlyRule() {
            final var mavenDependency = new MavenDependency(mock(Classification.class), mock(Classification.class), "dep");
            final var rule = new Rule("Pkg Only", dummyExpression, Rule.RuleScope.PACKAGE_ONLY);

            invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule));

            verify(ruleEvaluator, never()).evaluate(any(), any(), any());
        }

        @Test
        void enforceMavenMatchesRuleReferencingMissingDimension() {
            final var realEnforcer = new DefaultAllowRuleEnforcer(packageDependencyEvaluator, ScopeStrategy.MANUAL);
            final var source = mockClassification(domainA);
            final var target = mockClassification(domainA);
            final var mavenDependency = new MavenDependency(source, target, "test-artifact");
            final var rule = new Rule("Forbid non-business (misscoped)", notBusinessLayerRule, Rule.RuleScope.MODULE_ONLY);

            assertThrows(DependencyForbiddenException.class, () ->
                    invokeEnforceRules(realEnforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
        }
    }

    @Nested
    class PackageLevel {

        private RuleEvaluator ruleEvaluator;
        private DefaultAllowRuleEnforcer enforcer;

        @BeforeEach
        void setUp() {
            ruleEvaluator = mock(RuleEvaluator.class);
            enforcer = new TestableDefaultAllowRuleEnforcer(packageDependencyEvaluator, ruleEvaluator, ScopeStrategy.AUTOMATIC);
        }

        @Test
        void enforcePackageViolationMessage() {
            final var source = mockPackage("io.source");
            final var target = mockPackage("io.target");
            final var rule = new Rule("MyRule", dummyExpression, Rule.RuleScope.PACKAGE_ONLY);

            when(ruleEvaluator.evaluate(any(), any(), any())).thenReturn(true);

            final var result = new PackageDependencyEvaluationResult(true, "DEP FORBIDDEN");
            when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(result);

            final var ex = assertThrows(PackageDependencyForbiddenException.class, () ->
                    invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(rule)));

            assertThat(ex.getMessage()).contains("io.source -> io.target");
            assertThat(ex.getMessage()).contains("MyRule");
            assertThat(ex.getMessage()).contains("DEP FORBIDDEN");
        }

        @Test
        void enforcePackageAllowedWhenNoRealDependencyExists() {
            final var source = mockPackage("io.source");
            final var target = mockPackage("io.target");
            final var rule = new Rule("MyRule", dummyExpression, Rule.RuleScope.PACKAGE_ONLY);

            when(ruleEvaluator.evaluate(any(), any(), any())).thenReturn(true);
            when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(new PackageDependencyEvaluationResult(false, ""));

            assertDoesNotThrow(() ->
                    invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(rule)));
        }

        @Test
        void enforcePackageIgnoreModuleScope() {
            final var p1 = mockPackage("p1");
            final var p2 = mockPackage("p2");
            final var rule = new Rule("Mod Only", dummyExpression, Rule.RuleScope.MODULE_ONLY);

            invokeEnforceRules(enforcer, Set.of(), List.of(p1, p2), List.of(rule));

            verify(ruleEvaluator, never()).evaluate(any(), any(), any());
        }
    }

    private static Classification mockClassification(final Dimension.Partition... partitions) {
        final var classification = mock(Classification.class);
        when(classification.getPartitions()).thenReturn(Set.of(partitions));
        return classification;
    }

    private static PackageClassification mockPackage(final String path) {
        final var classification = mock(Classification.class);
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

    private static class TestableDefaultAllowRuleEnforcer extends DefaultAllowRuleEnforcer {
        private final RuleEvaluator ruleEvaluator;

        TestableDefaultAllowRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final RuleEvaluator ruleEvaluator, final ScopeStrategy scopeStrategy) {
            super(packageDependencyEvaluator, scopeStrategy);
            this.ruleEvaluator = ruleEvaluator;
        }

        @Override
        protected RuleEvaluator getRuleEvaluator() {
            return ruleEvaluator;
        }
    }
}
