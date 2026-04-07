package io.github.march_plugin.core.rules.enforcement;

import io.github.march_plugin.core.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.rules.exceptions.DependencyNotAllowedException;
import io.github.march_plugin.core.rules.exceptions.PackageDependencyNotAllowedException;
import io.github.march_plugin.core.rules.model.MavenDependency;
import io.github.march_plugin.core.rules.model.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Enforces all configured rules with strategy default deny.
 */
public class DefaultDenyRuleEnforcer extends RuleEnforcer {

    private final RuleEvaluator ruleEvaluator;

    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     * @param ruleEvaluator evaluates if a dependency matches a rule.
     */
    public DefaultDenyRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final RuleEvaluator ruleEvaluator) {
        super(packageDependencyEvaluator);
        this.ruleEvaluator = ruleEvaluator;
    }

    @Override
    public void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules) {
        var found = false;
        for (final var rule : rules) {
            if (ruleEvaluator.evaluate(rule.definition(), mavenDependency.source(), mavenDependency.target())) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new DependencyNotAllowedException(mavenDependency.description());
        }
    }

    @Override
    protected List<ForbiddenDependency> getForbiddenPackageDependencies(final Collection<ClassifiedPackage> packageClassifications, final List<Rule> rules) {
        final var forbiddenPackageDependencies = new ArrayList<ForbiddenDependency>();

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
