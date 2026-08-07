package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.enforcement.dependencies.ForbiddenDependency;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.rules.exceptions.DependencyNotAllowedException;
import io.github.march_plugin.core.enforcement.rules.exceptions.PackageDependencyNotAllowedException;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.config.rules.model.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Enforces all configured rules with strategy default deny.
 */
public class DefaultDenyRuleEnforcer extends RuleEnforcer {

    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     */
    public DefaultDenyRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator) {
        super(packageDependencyEvaluator);
    }

    @Override
    public void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules) {
        var found = false;
        for (final var rule : rules) {
            if (getRuleEvaluator().evaluate(rule.definition(), mavenDependency.source(), mavenDependency.target())) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DependencyNotAllowedException(mavenDependency.description());
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

                var isForbidden = true;
                for (final var rule : rules) {
                    if (ruleEvaluator.evaluate(rule.definition(), source.classification(), target.classification())) {
                        isForbidden = false;
                        break;
                    }
                }
                if (isForbidden) {
                    forbiddenPackageDependencies.add(new ForbiddenDependency(source, target, null));
                }
            }
        }

        return forbiddenPackageDependencies;
    }

    @Override
    protected void handlePackageDependencyViolation(final ForbiddenDependency forbiddenDependency, final String detail) {
        throw new PackageDependencyNotAllowedException(
                forbiddenDependency.source().packageHierarchy() + " -> " + forbiddenDependency.target().packageHierarchy(),
                detail);
    }
}
