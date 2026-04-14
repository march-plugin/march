package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.enforcement.dependencies.ForbiddenDependency;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluationResult;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.config.rules.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldFilterRulesByScopeCorrectWhenEnforcingAll() {
        final var globalRule = new Rule("Global", null, Rule.RuleScope.GLOBAL);
        final var packageRule = new Rule("Package Only", null, Rule.RuleScope.PACKAGE_ONLY);
        final var moduleRule = new Rule("Module Only", null, Rule.RuleScope.MODULE_ONLY);

        final var rules = List.of(globalRule, packageRule, moduleRule);
        final var mavenDependency = new MavenDependency(null, null, "desc");

        enforcer.enforceRules(Set.of(mavenDependency), Collections.emptyList(), rules);

        final var filteredMavenRules = enforcer.receivedMavenRules;

        assertThat(filteredMavenRules).contains(globalRule);
        assertThat(filteredMavenRules).contains(moduleRule);
        assertThat(filteredMavenRules).doesNotContain(packageRule);
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

    private static class TestRuleEnforcer extends RuleEnforcer {
        private List<Rule> receivedMavenRules;
        private List<ForbiddenDependency> mockForbiddenList = new ArrayList<>();
        private int violationCount = 0;
        private String lastDetail;

        public TestRuleEnforcer(final PackageDependencyEvaluator evaluator) {
            super(evaluator);
        }

        @Override
        protected void enforceRulesOnMavenDependencies(final MavenDependency dependency, final List<Rule> rules) {
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