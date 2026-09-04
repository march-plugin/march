package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.enforcement.dependencies.ForbiddenDependency;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.rules.exceptions.DependencyForbiddenException;
import io.github.march_plugin.core.enforcement.rules.exceptions.PackageDependencyForbiddenException;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.config.rules.model.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Enforces all configured rules.
 */
public class DefaultAllowRuleEnforcer extends RuleEnforcer {


    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     * @param scopeStrategy whether module-level rule applicability is determined automatically or manually
     */
    public DefaultAllowRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final ScopeStrategy scopeStrategy) {
        super(packageDependencyEvaluator, scopeStrategy);
    }

    @Override
    public void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules, final Collection<PackageClassification> packageClassifications) {
        for (final var rule : rules) {
            if (matchesAtModuleLevel(rule, mavenDependency.source(), mavenDependency.target())) {
                throw new DependencyForbiddenException(mavenDependency.description(), mavenDependency.source(), mavenDependency.target(), rule.description());
            }
        }
    }

    @Override
    protected List<ForbiddenDependency> getForbiddenPackageDependencies(final Collection<PackageClassification> packageClassifications, final List<Rule> rules) {
        final var forbiddenPackageDependencies = new ArrayList<ForbiddenDependency>();
        final var ruleEvaluator = getRuleEvaluator();

        for (final var source : packageClassifications) {
            for (final var target : packageClassifications) {
                if (source.equals(target)) {
                    continue;
                }

                for (final var rule : rules) {
                    if (ruleEvaluator.evaluate(rule.definition(), source.classification(), target.classification())) {
                        forbiddenPackageDependencies.add(new ForbiddenDependency(source, target, rule));
                        break;
                    }
                }
            }
        }
        return forbiddenPackageDependencies;
    }

    @Override
    protected void handlePackageDependencyViolation(final ForbiddenDependency forbiddenDependency, final String detail) {
        throw new PackageDependencyForbiddenException(
                forbiddenDependency.source().packageHierarchy() + " -> " + forbiddenDependency.target().packageHierarchy(),
                forbiddenDependency.source().classification(),
                forbiddenDependency.target().classification(),
                forbiddenDependency.rule().description(),
                detail);

    }
}
