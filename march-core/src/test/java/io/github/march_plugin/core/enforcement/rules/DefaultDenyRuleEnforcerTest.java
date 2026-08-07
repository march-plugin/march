package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
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
import static org.mockito.Mockito.when;

class DefaultDenyRuleEnforcerTest {

    private RuleEvaluator ruleEvaluator;
    private PackageDependencyEvaluator packageDependencyEvaluator;
    private DefaultDenyRuleEnforcer enforcer;

    private final LogicalExpression dummyExpression = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.Equal(new PartitionExpression.Null(), new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, null))
    );

    @BeforeEach
    void setUp() {
        ruleEvaluator = mock(RuleEvaluator.class);
        packageDependencyEvaluator = mock(PackageDependencyEvaluator.class);
        enforcer = new TestableDefaultDenyRuleEnforcer(packageDependencyEvaluator, ruleEvaluator);
    }

    @Test
    void enforceMavenViolationWhenNoRulesMatch() {
        final var source = mock(Classification.class);
        final var target = mock(Classification.class);
        final var mavenDependency = new MavenDependency(source, target, "test-artifact");
        final var rule = new Rule("Some Rule", dummyExpression, Rule.RuleScope.GLOBAL);

        when(ruleEvaluator.evaluate(any(), eq(source), eq(target))).thenReturn(false);

        assertThrows(DependencyNotAllowedException.class, () ->
                invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
    }

    @Test
    void enforceMavenAllowedWhenRuleMatches() {
        final var source = mock(Classification.class);
        final var target = mock(Classification.class);
        final var mavenDependency = new MavenDependency(source, target, "test-artifact");
        final var rule = new Rule("Allow Rule", dummyExpression, Rule.RuleScope.GLOBAL);

        when(ruleEvaluator.evaluate(any(), eq(source), eq(target))).thenReturn(true);

        assertDoesNotThrow(() ->
                invokeEnforceRules(enforcer, Set.of(mavenDependency), List.of(), List.of(rule)));
    }

    @Test
    void enforcePackageViolationMessage() {
        final var source = mockPackage("io.source");
        final var target = mockPackage("io.target");
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
        final var source = mockPackage("io.source");
        final var target = mockPackage("io.target");
        final var rule = new Rule("Allow Pkg", dummyExpression, Rule.RuleScope.PACKAGE_ONLY);

        final var result = new PackageDependencyEvaluationResult(true, "Actual code dependency exists");
        when(packageDependencyEvaluator.evaluateForbiddenDependency(any())).thenReturn(result);

        when(ruleEvaluator.evaluate(any(), any(), any())).thenReturn(true);

        assertDoesNotThrow(() ->
                invokeEnforceRules(enforcer, Set.of(), List.of(source, target), List.of(rule)));
    }

    private PackageClassification mockPackage(final String path) {
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

    private static class TestableDefaultDenyRuleEnforcer extends DefaultDenyRuleEnforcer {
        private final RuleEvaluator ruleEvaluator;

        TestableDefaultDenyRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final RuleEvaluator ruleEvaluator) {
            super(packageDependencyEvaluator);
            this.ruleEvaluator = ruleEvaluator;
        }

        @Override
        protected RuleEvaluator getRuleEvaluator() {
            return ruleEvaluator;
        }
    }
}