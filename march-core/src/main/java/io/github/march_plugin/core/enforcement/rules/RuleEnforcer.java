package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.enforcement.dependencies.ForbiddenDependency;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.project.ProjectModuleRegistry;

import java.util.Collection;
import java.util.List;

/**
 * Enforces the configured rules on all modules and packages.
 */
public abstract class RuleEnforcer {

    private final PackageDependencyEvaluator packageDependencyEvaluator;
    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();

    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     */
    public RuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator) {
        this.packageDependencyEvaluator = packageDependencyEvaluator;
    }

    protected RuleEvaluator getRuleEvaluator() {
        return ruleEvaluator;
    }

    /**
     * Enforces all configured rules across the modules and packages of the project.
     *
     * @param classificationRegistry the registry containing all classified modules and packages
     * @param projectModuleRegistry the registry of all actually existing maven modules, used to resolve their dependencies
     * @param ruleRegistry the registry containing the configured rules
     */
    public void enforceRules(final ClassificationRegistry classificationRegistry, final ProjectModuleRegistry projectModuleRegistry, final RuleRegistry ruleRegistry) {
        final var dependencies = projectModuleRegistry.getDependencies(classificationRegistry);
        final var packageClassifications = classificationRegistry.getAllClassifiedPackages().stream().map(ClassifiedPackage::getClassifiedPackage).toList();;

        for (final var dependency : dependencies) {
            enforceRulesOnMavenDependencies(dependency, ruleRegistry.getRules().stream().filter(r -> !r.ruleScope().equals(Rule.RuleScope.PACKAGE_ONLY)).toList());
        }

        enforceRulesOnPackageDependencies(packageClassifications, ruleRegistry.getRules().stream().filter(r -> !r.ruleScope().equals(Rule.RuleScope.MODULE_ONLY)).toList());
    }

    /**
     * Enforces all rules across the packages of the project.
     *
     * @param packageClassifications the packages classified in the project
     * @param rules                  the rules configured in march registry
     */
    protected void enforceRulesOnPackageDependencies(final Collection<PackageClassification> packageClassifications, final List<Rule> rules) {
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
    protected abstract List<ForbiddenDependency> getForbiddenPackageDependencies(final Collection<PackageClassification> packageClassifications, final List<Rule> rules);

    /**
     * Handles the detection of a found dependency.
     * @param forbiddenDependency the forbidden dependency found
     * @param detail details about the dependency violation
     */
    protected abstract void handlePackageDependencyViolation(final ForbiddenDependency forbiddenDependency, final String detail);
}
