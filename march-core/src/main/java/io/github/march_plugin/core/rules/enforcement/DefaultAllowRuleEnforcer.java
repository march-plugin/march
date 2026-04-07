package io.github.march_plugin.core.rules.enforcement;

import io.github.march_plugin.core.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.rules.exceptions.DependencyForbiddenException;
import io.github.march_plugin.core.rules.exceptions.PackageDependencyForbiddenException;
import io.github.march_plugin.core.rules.model.MavenDependency;
import io.github.march_plugin.core.rules.model.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Enforces all configured rules.
 */
public class DefaultAllowRuleEnforcer extends RuleEnforcer {

    private final RuleEvaluator ruleEvaluator;

    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     * @param ruleEvaluator evaluates if a dependency matches a rule.
     */
    public DefaultAllowRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final RuleEvaluator ruleEvaluator) {
        super(packageDependencyEvaluator);
        this.ruleEvaluator = ruleEvaluator;
    }

    @Override
    public void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules) {
        for (final var rule : rules) {
            if (ruleEvaluator.evaluate(rule.definition(), mavenDependency.source(), mavenDependency.target())) {
                throw new DependencyForbiddenException(mavenDependency.toString(), rule.description());
            }
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
                forbiddenDependency.rule().description(),
                detail);

    }
}
