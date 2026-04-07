package io.github.march_plugin.core.rules.enforcement;

import io.github.march_plugin.core.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.rules.model.MavenDependency;
import io.github.march_plugin.core.rules.model.Rule;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Enforces the configured rules on all modules and packages.
 */
public abstract class RuleEnforcer {

    private final PackageDependencyEvaluator packageDependencyEvaluator;

    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     */
    public RuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator) {
        this.packageDependencyEvaluator = packageDependencyEvaluator;
    }

    /**
     * Enforces all rules across the project.
     *
     * @param dependencies           the maven dependencies of the project
     * @param packageClassifications the packages classified in the project
     * @param rules                  the rules configured in march registry
     */
    public void enforceRules(final Set<MavenDependency> dependencies, final Collection<ClassifiedPackage> packageClassifications, final List<Rule> rules) {
        for (final var dependency : dependencies) {
            enforceRulesOnMavenDependencies(dependency, rules.stream().filter(r -> !r.ruleScope().equals(Rule.RuleScope.PACKAGE_ONLY)).toList());
        }

        enforceRulesOnPackageDependencies(packageClassifications, rules.stream().filter(r -> !r.ruleScope().equals(Rule.RuleScope.MODULE_ONLY)).toList());
    }

    /**
     * Enforces all rules across the packages of the project.
     *
     * @param packageClassifications the packages classified in the project
     * @param rules                  the rules configured in march registry
     */
    protected void enforceRulesOnPackageDependencies(final Collection<ClassifiedPackage> packageClassifications, final List<Rule> rules) {
        final var forbiddenPackageDependencies = getForbiddenPackageDependencies(packageClassifications, rules);

        for (final var forbiddenPackageDependency : forbiddenPackageDependencies) {
            final var evaluationResult = packageDependencyEvaluator.evaluateForbiddenDependency(forbiddenPackageDependency);
            if (evaluationResult.containsViolation()) {
                handlePackageDependencyViolation(
                        forbiddenPackageDependency,
                        evaluationResult.detail());
            }
        }
    }

    /**
     * Enforces the rules for a maven dependency.
     *
     * @param mavenDependency the maven dependency to check
     * @param rules           the rules configured in march registry
     */
    protected abstract void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules);

    /**
     * Gets all forbidden dependencies between packages .
     *
     * @param packageClassifications the packages classified in the project
     * @param rules                  the rules configured in march registry
     * @return all forbidden dependencies between packages
     */
    protected abstract List<ForbiddenDependency> getForbiddenPackageDependencies(final Collection<ClassifiedPackage> packageClassifications, final List<Rule> rules);

    /**
     * Handles the detection of a found dependency.
     * @param forbiddenDependency the forbidden dependency found
     * @param detail details about the dependency violation
     */
    protected abstract void handlePackageDependencyViolation(final ForbiddenDependency forbiddenDependency, final String detail);
}
