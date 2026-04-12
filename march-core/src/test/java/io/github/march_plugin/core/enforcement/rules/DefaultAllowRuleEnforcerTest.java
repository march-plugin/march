package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.projectstructure.PackageHierarchy;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluationResult;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.config.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.enforcement.rules.exceptions.DependencyForbiddenException;
import io.github.march_plugin.core.enforcement.rules.exceptions.PackageDependencyForbiddenException;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAllowRuleEnforcerTest {

    private RuleEvaluator ruleEvaluator;
    private PackageDependencyEvaluator packageDependencyEvaluator;
    private DefaultAllowRuleEnforcer enforcer;

    private final LogicalExpression dummyExpression = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.Equal(new PartitionExpression.Null(), new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, null))
    );

    @BeforeEach
    void setUp() {
        ruleEvaluator = mock(RuleEvaluator.class);
        packageDependencyEvaluator = mock(PackageDependencyEvaluator.class);
        enforcer = new DefaultAllowRuleEnforcer(packageDependencyEvaluator, ruleEvaluator);
    }

    @Test
    void enforceMavenViolation() {
        final var source = mock(Classification.class);
        final var target = mock(Classification.class);
        final var mavenDependency = new MavenDependency(source, target, "test-artifact");
        final var rule = new Rule("Forbidden Dep", dummyExpression, Rule.RuleScope.GLOBAL);

        when(ruleEvaluator.evaluate(any(), eq(source), eq(target))).thenReturn(true);

        assertThrows(DependencyForbiddenException.class, () ->
                enforcer.enforceRules(Set.of(mavenDependency), List.of(), List.of(rule)));
    }

    @Test
    void enforceMavenIgnorePackageScope() {
        final var mavenDependency = new MavenDependency(mock(Classification.class), mock(Classification.class), "dep");
        final var rule = new Rule("Pkg Only", dummyExpression, Rule.RuleScope.PACKAGE_ONLY);

        enforcer.enforceRules(Set.of(mavenDependency), List.of(), List.of(rule));

        verify(ruleEvaluator, never()).evaluate(any(), any(), any());
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
                enforcer.enforceRules(Set.of(), List.of(source, target), List.of(rule)));

        assertThat(ex.getMessage()).contains("io.source -> io.target");
        assertThat(ex.getMessage()).contains("MyRule");
        assertThat(ex.getMessage()).contains("DEP FORBIDDEN");
    }

    @Test
    void enforcePackageIgnoreModuleScope() {
        final var p1 = mockPackage("p1");
        final var p2 = mockPackage("p2");
        final var rule = new Rule("Mod Only", dummyExpression, Rule.RuleScope.MODULE_ONLY);

        enforcer.enforceRules(Set.of(), List.of(p1, p2), List.of(rule));

        verify(ruleEvaluator, never()).evaluate(any(), any(), any());
    }

    private ClassifiedPackage mockPackage(final String path) {
        final var classification = mock(Classification.class);
        final var hierarchy = new PackageHierarchy(List.of(path.split("\\.")));
        return new ClassifiedPackage(classification, hierarchy, true);
    }
}