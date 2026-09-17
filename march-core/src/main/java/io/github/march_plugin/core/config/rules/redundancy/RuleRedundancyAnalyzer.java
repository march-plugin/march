package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.model.Rule;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds rules that are redundant: rules whose removal would not change the outcome for any dependency,
 * because every dependency they permit or forbid is, over the full classification domain, already
 * permitted or forbidden by the remaining rules of the same {@link Rule.RuleScope} context.
 */
public final class RuleRedundancyAnalyzer {

    /**
     * Analyzes all rules for reachability.
     *
     * @param rules                 the rules to analyze
     * @param projectStructureRoot  the modularity tree root, or {@code null} to leave every classification unrestricted
     * @return the rules found to be unreachable
     */
    public List<Rule> findUnreachableRules(final List<Rule> rules, final ModuleModularity projectStructureRoot) {
        final var moduleContext = effectiveRules(rules, Rule.RuleScope.MODULE_ONLY);
        final var packageContext = effectiveRules(rules, Rule.RuleScope.PACKAGE_ONLY);

        final var moduleUnreachable = unreachableWithin(moduleContext, projectStructureRoot);
        final var packageUnreachable = unreachableWithin(packageContext, projectStructureRoot);

        return rules.stream()
                .filter(rule -> isInBoth(rule, moduleUnreachable, packageUnreachable))
                .toList();
    }

    /**
     * Analyzes rules for redundancy.
     *
     * @param rulesToCheck          the rules to analyze
     * @param projectStructureRoot  the modularity tree root, or {@code null} to leave every classification unrestricted
     * @return the rules found to be redundant, in their original order
     */
    public List<Rule> findRedundantRules(final List<Rule> rulesToCheck, final ModuleModularity projectStructureRoot) {
        final var moduleContext = effectiveRules(rulesToCheck, Rule.RuleScope.MODULE_ONLY);
        final var packageContext = effectiveRules(rulesToCheck, Rule.RuleScope.PACKAGE_ONLY);

        final var redundantInModuleContext = redundantWithin(moduleContext, projectStructureRoot);
        final var redundantInPackageContext = redundantWithin(packageContext, projectStructureRoot);

        return rulesToCheck.stream()
                .filter(rule -> isInBoth(rule, redundantInModuleContext, redundantInPackageContext))
                .toList();
    }

    private boolean isInBoth(final Rule rule, final Set<Rule> inModuleContext, final Set<Rule> inPackageContext) {
        return switch (rule.ruleScope()) {
            case MODULE_ONLY -> inModuleContext.contains(rule);
            case PACKAGE_ONLY -> inPackageContext.contains(rule);
            case GLOBAL -> inModuleContext.contains(rule) && inPackageContext.contains(rule);
        };
    }

    private List<Rule> effectiveRules(final List<Rule> rules, final Rule.RuleScope scope) {
        return rules.stream()
                .filter(rule -> rule.ruleScope() == Rule.RuleScope.GLOBAL || rule.ruleScope() == scope)
                .toList();
    }

    private Set<Rule> unreachableWithin(final List<Rule> rules, final ModuleModularity projectStructureRoot) {
        final var encoder = new RuleSatEncoder(rules, projectStructureRoot);
        final var solver = new SatSolverBuilder(encoder.variables()).build();

        final var unreachable = new HashSet<Rule>();
        for (final var candidate : rules) {
            if (!isSatisfiable(encoder.assumptionsForReachability(candidate), solver)) {
                unreachable.add(candidate);
            }
        }
        return unreachable;
    }

    private Set<Rule> redundantWithin(final List<Rule> rules, final ModuleModularity projectStructureRoot) {
        final var encoder = new RuleSatEncoder(rules, projectStructureRoot);
        final var solver = new SatSolverBuilder(encoder.variables()).build();

        final var redundant = new HashSet<Rule>();
        for (final var candidate : rules) {
            if (!isSatisfiable(encoder.assumptionsFor(candidate, rules), solver)) {
                redundant.add(candidate);
            }
        }
        return redundant;
    }

    private boolean isSatisfiable(final int[] assumptions, final ISolver solver) {
        try {
            return solver.isSatisfiable(new VecInt(assumptions));
        } catch (final TimeoutException e) {
            return true;
        }
    }

}
