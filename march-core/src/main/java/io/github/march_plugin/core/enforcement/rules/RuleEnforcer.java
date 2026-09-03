package io.github.march_plugin.core.enforcement.rules;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.config.rules.evaluation.RuleReducer;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;
import io.github.march_plugin.core.enforcement.dependencies.ForbiddenDependency;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.project.MavenDependency;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.project.ProjectModuleRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Enforces the configured rules on all modules and packages.
 */
public abstract class RuleEnforcer {

    private final PackageDependencyEvaluator packageDependencyEvaluator;
    private final ScopeStrategy scopeStrategy;
    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final RuleReducer ruleReducer = new RuleReducer();

    /**
     * Constructs the RuleEnforcer.
     *
     * @param packageDependencyEvaluator evaluates if a forbidden package dependency is present
     * @param scopeStrategy the configured scope strategy
     */
    public RuleEnforcer(final PackageDependencyEvaluator packageDependencyEvaluator, final ScopeStrategy scopeStrategy) {
        this.packageDependencyEvaluator = packageDependencyEvaluator;
        this.scopeStrategy = scopeStrategy;
    }

    protected RuleEvaluator getRuleEvaluator() {
        return ruleEvaluator;
    }

    protected ScopeStrategy getScopeStrategy() {
        return scopeStrategy;
    }

    /**
     * Checks whether a rule matches a module-level dependency, according to the configured {@link ScopeStrategy}.
     *
     * @param rule the rule to check
     * @param source the source classification
     * @param target the target classification
     * @return whether the rule matches at module level
     */
    protected boolean matchesAtModuleLevel(final Rule rule, final Classification source, final Classification target) {
        return switch (scopeStrategy) {
            case AUTOMATIC -> ruleReducer.reduce(rule.definition(), source.getPartitions(), target.getPartitions(), Set.of(), Set.of())
                    instanceof EvaluatedLogicalExpression.AlwaysTrue;
            case MANUAL -> !rule.ruleScope().equals(Rule.RuleScope.PACKAGE_ONLY)
                    && ruleEvaluator.evaluate(rule.definition(), source, target);
        };
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
            enforceRulesOnMavenDependencies(dependency, ruleRegistry.getRules(), packageClassifications);
        }

        enforceRulesOnPackageDependencies(packageClassifications, ruleRegistry.getRules().stream().filter(r -> !Rule.RuleScope.MODULE_ONLY.equals(r.ruleScope())).toList());
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
     * @param packageClassifications all packages classified in the project
     */
    protected abstract void enforceRulesOnMavenDependencies(final MavenDependency mavenDependency, final List<Rule> rules, final Collection<PackageClassification> packageClassifications);

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
