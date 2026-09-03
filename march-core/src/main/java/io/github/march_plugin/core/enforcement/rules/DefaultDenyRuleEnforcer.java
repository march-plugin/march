package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
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
     * @param scopeStrategy whether module-level rule applicability is determined automatically or manually
     */
    public DefaultDenyRuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final ScopeStrategy scopeStrategy) {
        super(packageDependencyEvaluator, scopeStrategy);
    }

    @Override
    public void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules, final Collection<PackageClassification> packageClassifications) {
        final var source = mavenDependency.source();
        final var target = mavenDependency.target();

        for (final var rule : rules) {
            if (matchesAtModuleLevel(rule, source, target)) {
                return;
            }
        }

        if (getScopeStrategy() == ScopeStrategy.AUTOMATIC) {
            if (isSomePackageDependencyAllowed(source, target, rules, packageClassifications)) {
                return;
            }
        }

        throw new DependencyNotAllowedException(mavenDependency.description());
    }

    private boolean isSomePackageDependencyAllowed(final Classification source, final Classification target, final List<Rule> rules, final Collection<PackageClassification> packageClassifications) {
        final var ruleEvaluator = getRuleEvaluator();
        final var sourcePackages = belongingTo(source, packageClassifications);
        final var targetPackages = belongingTo(target, packageClassifications);

        for (final var sourcePackage : sourcePackages) {
            for (final var targetPackage : targetPackages) {
                for (final var rule : rules) {
                    if (ruleEvaluator.evaluate(rule.definition(), sourcePackage.classification(), targetPackage.classification())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<PackageClassification> belongingTo(final Classification moduleClassification, final Collection<PackageClassification> packageClassifications) {
        return packageClassifications.stream()
                .filter(p -> p.classification().getPartitions().containsAll(moduleClassification.getPartitions()))
                .toList();
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
