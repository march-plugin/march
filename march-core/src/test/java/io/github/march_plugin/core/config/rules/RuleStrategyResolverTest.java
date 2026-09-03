package io.github.march_plugin.core.config.rules;

import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.rules.DefaultAllowRuleEnforcer;
import io.github.march_plugin.core.enforcement.rules.DefaultDenyRuleEnforcer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RuleStrategyResolverTest {

    private final PackageDependencyEvaluator packageDependencyEvaluator = mock(PackageDependencyEvaluator.class);

    @Test
    void shouldResolveDefaultDenyEnforcer() {
        final var resolver = new RuleStrategyResolver(RuleStrategy.DEFAULT_DENY, ScopeStrategy.AUTOMATIC);

        final var enforcer = resolver.getRuleEnforcer(packageDependencyEvaluator);

        assertThat(enforcer).isInstanceOf(DefaultDenyRuleEnforcer.class);
    }

    @Test
    void shouldResolveDefaultAllowEnforcer() {
        final var resolver = new RuleStrategyResolver(RuleStrategy.DEFAULT_ALLOW, ScopeStrategy.MANUAL);

        final var enforcer = resolver.getRuleEnforcer(packageDependencyEvaluator);

        assertThat(enforcer).isInstanceOf(DefaultAllowRuleEnforcer.class);
    }
}
