package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.rules.config.DependencyConfig;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import io.github.march_plugin.core.enforcement.dependencies.ForbiddenDependency;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluationResult;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.rules.exceptions.NonLeafMavenDependencyException;
import io.github.march_plugin.core.enforcement.rules.exceptions.NonLeafPackageDependencyException;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.project.ProjectModuleRegistry;
import io.github.march_plugin.core.config.rules.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEnforcerTest {

    private PackageDependencyEvaluator evaluator;
    private TestRuleEnforcer enforcer;

    @BeforeEach
    void setUp() {
        evaluator = mock(PackageDependencyEvaluator.class);
        enforcer = new TestRuleEnforcer(evaluator);
    }

    @Test
    void shouldPassAllRulesUnfilteredWhenEnforcingMavenDependencies() {
        final var globalRule = new Rule("Global", null, Rule.RuleScope.GLOBAL);
        final var packageRule = new Rule("Package Only", null, Rule.RuleScope.PACKAGE_ONLY);
        final var moduleRule = new Rule("Module Only", null, Rule.RuleScope.MODULE_ONLY);

        final var rules = List.of(globalRule, packageRule, moduleRule);
        final var mavenDependency = new MavenDependency(null, null, true, true, "desc");

        invokeEnforceRules(enforcer, Set.of(mavenDependency), Collections.emptyList(), rules);

        assertThat(enforcer.receivedMavenRules).containsExactlyInAnyOrder(globalRule, packageRule, moduleRule);
    }

    @Test
    void shouldHandleViolations_OnlyWhenEvaluatorReturnsTrue() {
        final var packageClassification = mock(PackageClassification.class);
        final var rule = new Rule("Check", null, Rule.RuleScope.GLOBAL);
        final var forbidden = new ForbiddenDependency(packageClassification, packageClassification, rule);

        enforcer.mockForbiddenList = List.of(forbidden);

        when(evaluator.evaluateForbiddenDependency(forbidden))
                .thenReturn(new PackageDependencyEvaluationResult(true, "Error Detail"));

        enforcer.enforceRulesOnPackageDependencies(List.of(packageClassification), List.of(rule));

        assertThat(enforcer.violationCount).isEqualTo(1);
        assertThat(enforcer.lastDetail).isEqualTo("Error Detail");
    }

    @Test
    void matchingRulesAtModuleLevelReturnsAllMatchingNonPackageOnlyRulesInManualMode() {
        final var domainBuilder = new Dimension.Builder("domain");
        final var domainA = domainBuilder.addPartition("a");
        final var domainB = domainBuilder.addPartition("b");
        final var domain = domainBuilder.build();

        final var sameDomainExpression = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, domain),
                new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, domain)));

        final var globalRule = new Rule("Same domain (global)", sameDomainExpression, Rule.RuleScope.GLOBAL);
        final var moduleOnlyRule = new Rule("Same domain (module only)", sameDomainExpression, Rule.RuleScope.MODULE_ONLY);
        final var packageOnlyRule = new Rule("Same domain (package only)", sameDomainExpression, Rule.RuleScope.PACKAGE_ONLY);

        final var manualEnforcer = new TestRuleEnforcer(evaluator, ScopeStrategy.MANUAL);
        final var source = new Classification.Builder().addPartition(domainA).build();
        final var target = new Classification.Builder().addPartition(domainA).build();

        final var matches = manualEnforcer.matchingRulesAtModuleLevel(List.of(globalRule, moduleOnlyRule, packageOnlyRule), source, target);

        assertThat(matches).containsExactly(globalRule, moduleOnlyRule);
    }

    @Test
    void matchingRulesAtModuleLevelReturnsEmptyWhenNoRuleMatches() {
        final var domainBuilder = new Dimension.Builder("domain");
        final var domainA = domainBuilder.addPartition("a");
        final var domainB = domainBuilder.addPartition("b");
        final var domain = domainBuilder.build();

        final var sameDomainExpression = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, domain),
                new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, domain)));
        final var globalRule = new Rule("Same domain", sameDomainExpression, Rule.RuleScope.GLOBAL);

        final var manualEnforcer = new TestRuleEnforcer(evaluator, ScopeStrategy.MANUAL);
        final var source = new Classification.Builder().addPartition(domainA).build();
        final var target = new Classification.Builder().addPartition(domainB).build();

        final var matches = manualEnforcer.matchingRulesAtModuleLevel(List.of(globalRule), source, target);

        assertThat(matches).isEmpty();
    }

    private static void invokeEnforceRules(final RuleEnforcer enforcer, final Set<MavenDependency> dependencies,
                                            final Collection<PackageClassification> packages, final List<Rule> rules) {
        invokeEnforceRules(enforcer, dependencies, packages, rules, null);
    }

    private static void invokeEnforceRules(final RuleEnforcer enforcer, final Set<MavenDependency> dependencies,
                                            final Collection<PackageClassification> packages, final List<Rule> rules,
                                            final DependencyConfig dependencyConfig) {
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
        when(ruleRegistry.getDependencyConfig()).thenReturn(dependencyConfig);

        enforcer.enforceRules(classificationRegistry, projectModuleRegistry, ruleRegistry);
    }

    @Nested
    class EnforceLeavesOnly {

        private static PackageClassification mockPackage(final String path, final boolean isLeaf) {
            final var classification = mock(Classification.class);
            final var hierarchy = new PackageHierarchy(List.of(path.split("\\.")));
            return new PackageClassification(classification, hierarchy, isLeaf);
        }

        @Test
        void shouldThrowWhenMavenDependencySourceIsNonLeaf() {
            final var dependency = new MavenDependency(null, null, false, true, "desc");

            assertThatThrownBy(() -> invokeEnforceRules(enforcer, Set.of(dependency), List.of(), List.of(), DependencyConfig.LEAVES_ONLY))
                    .isInstanceOf(NonLeafMavenDependencyException.class);
        }

        @Test
        void shouldThrowWhenMavenDependencyTargetIsNonLeaf() {
            final var dependency = new MavenDependency(null, null, true, false, "desc");

            assertThatThrownBy(() -> invokeEnforceRules(enforcer, Set.of(dependency), List.of(), List.of(), DependencyConfig.LEAVES_ONLY))
                    .isInstanceOf(NonLeafMavenDependencyException.class);
        }

        @Test
        void shouldNotThrowWhenBothMavenDependencySidesAreLeaves() {
            final var dependency = new MavenDependency(null, null, true, true, "desc");

            assertThatCode(() -> invokeEnforceRules(enforcer, Set.of(dependency), List.of(), List.of(), DependencyConfig.LEAVES_ONLY))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldNotEnforceLeavesOnlyWhenDependencyConfigIsAnyLevel() {
            final var dependency = new MavenDependency(null, null, false, false, "desc");

            assertThatCode(() -> invokeEnforceRules(enforcer, Set.of(dependency), List.of(), List.of(), DependencyConfig.ANY_LEVEL))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowWhenANonLeafPackagePairHasARealBytecodeDependency() {
            final var source = mockPackage("a", false);
            final var target = mockPackage("b", true);
            when(evaluator.evaluateForbiddenDependency(new ForbiddenDependency(source, target, null)))
                    .thenReturn(new PackageDependencyEvaluationResult(true, "detail"));

            assertThatThrownBy(() -> invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(), DependencyConfig.LEAVES_ONLY))
                    .isInstanceOf(NonLeafPackageDependencyException.class);
        }

        @Test
        void shouldNotThrowWhenANonLeafPackagePairHasNoRealBytecodeDependency() {
            final var source = mockPackage("a", false);
            final var target = mockPackage("b", true);
            when(evaluator.evaluateForbiddenDependency(new ForbiddenDependency(source, target, null)))
                    .thenReturn(new PackageDependencyEvaluationResult(false, null));
            when(evaluator.evaluateForbiddenDependency(new ForbiddenDependency(target, source, null)))
                    .thenReturn(new PackageDependencyEvaluationResult(false, null));

            assertThatCode(() -> invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(), DependencyConfig.LEAVES_ONLY))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldNotCheckPackagePairsWhereBothSidesAreLeaves() {
            final var source = mockPackage("a", true);
            final var target = mockPackage("b", true);

            assertThatCode(() -> invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(), DependencyConfig.LEAVES_ONLY))
                    .doesNotThrowAnyException();
        }
    }

    private static class TestRuleEnforcer extends RuleEnforcer {
        private List<Rule> receivedMavenRules;
        private List<ForbiddenDependency> mockForbiddenList = new ArrayList<>();
        private int violationCount = 0;
        private String lastDetail;

        public TestRuleEnforcer(final PackageDependencyEvaluator evaluator) {
            super(evaluator, ScopeStrategy.AUTOMATIC);
        }

        public TestRuleEnforcer(final PackageDependencyEvaluator evaluator, final ScopeStrategy scopeStrategy) {
            super(evaluator, scopeStrategy);
        }

        @Override
        protected void enforceRulesOnMavenDependencies(final MavenDependency dependency, final List<Rule> rules, final Collection<PackageClassification> packageClassifications) {
            this.receivedMavenRules = rules;
        }

        @Override
        protected List<ForbiddenDependency> getForbiddenPackageDependencies(final Collection<PackageClassification> pkgs, final List<Rule> rules) {
            return mockForbiddenList;
        }

        @Override
        protected void handlePackageDependencyViolation(final ForbiddenDependency forbidden, final String detail) {
            this.violationCount++;
            this.lastDetail = detail;
        }
    }
}