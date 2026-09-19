package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.DependencyConfig;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks whether two rule sets decide every dependency the same way.
 */
public final class RuleSetEquivalenceChecker {

    /**
     * Checks whether {@code rulesA} and {@code rulesB} decide every dependency the same way.
     *
     * @param rulesA                the first rule set
     * @param rulesB                the second rule set
     * @param projectStructureRoot  the modularity tree root shared by both rule sets, or {@code null} to leave every classification unrestricted
     * @param ruleStrategyA         {@code rulesA}'s configured rule strategy
     * @param ruleStrategyB         {@code rulesB}'s configured rule strategy
     * @param scopeStrategyA        {@code rulesA}'s configured scope strategy
     * @param scopeStrategyB        {@code rulesB}'s configured scope strategy
     * @param dependencyConfig      whether possible dependencies are restricted to leaves only
     * @return {@code true} if no classification exists where the two rule sets would disagree
     */
    public boolean areEquivalent(final List<Rule> rulesA, final List<Rule> rulesB, final ModuleModularity projectStructureRoot, final RuleStrategy ruleStrategyA, final RuleStrategy ruleStrategyB, final ScopeStrategy scopeStrategyA, final ScopeStrategy scopeStrategyB, final DependencyConfig dependencyConfig) {
        return findDisagreement(rulesA, rulesB, projectStructureRoot, ruleStrategyA, ruleStrategyB, scopeStrategyA, scopeStrategyB, dependencyConfig).isEmpty();
    }

    /**
     * Finds a concrete classification where {@code rulesA} and {@code rulesB} would decide a dependency differently.
     *
     * @param rulesA                the first rule set
     * @param rulesB                the second rule set
     * @param projectStructureRoot  the modularity tree root shared by both rule sets, or {@code null} to leave every classification unrestricted
     * @param ruleStrategyA         {@code rulesA}'s configured rule strategy
     * @param ruleStrategyB         {@code rulesB}'s configured rule strategy
     * @param scopeStrategyA        {@code rulesA}'s configured scope strategy
     * @param scopeStrategyB        {@code rulesB}'s configured scope strategy
     * @param dependencyConfig      whether possible dependencies are restricted to leaves only
     * @return a witnessing disagreement, or empty if the two rule sets are equivalent
     */
    public Optional<Disagreement> findDisagreement(final List<Rule> rulesA, final List<Rule> rulesB, final ModuleModularity projectStructureRoot, final RuleStrategy ruleStrategyA, final RuleStrategy ruleStrategyB, final ScopeStrategy scopeStrategyA, final ScopeStrategy scopeStrategyB, final DependencyConfig dependencyConfig) {
        final var samePolarity = ruleStrategyA == ruleStrategyB;
        for (final var contextScope : List.of(Rule.RuleScope.MODULE_ONLY, Rule.RuleScope.PACKAGE_ONLY)) {
            final var disagreement = disagreementWithin(rulesA, rulesB, projectStructureRoot, contextScope, scopeStrategyA, scopeStrategyB, samePolarity, dependencyConfig);
            if (disagreement.isPresent()) {
                return disagreement;
            }
        }
        return Optional.empty();
    }

    private Optional<Disagreement> disagreementWithin(final List<Rule> rulesA, final List<Rule> rulesB, final ModuleModularity projectStructureRoot, final Rule.RuleScope contextScope, final ScopeStrategy scopeStrategyA, final ScopeStrategy scopeStrategyB, final boolean samePolarity, final DependencyConfig dependencyConfig) {
        final var contextRulesA = RuleScopeFilter.effectiveRules(rulesA, contextScope, scopeStrategyA);
        final var contextRulesB = RuleScopeFilter.effectiveRules(rulesB, contextScope, scopeStrategyB);

        final var combined = new ArrayList<Rule>(contextRulesA.size() + contextRulesB.size());
        combined.addAll(contextRulesA);
        combined.addAll(contextRulesB);

        final var encoder = new RuleSatEncoder(combined, projectStructureRoot, contextScope == Rule.RuleScope.MODULE_ONLY, dependencyConfig);

        final var someAMatches = someRuleMatches(encoder, contextRulesA);
        final var someBMatches = someRuleMatches(encoder, contextRulesB);

        final var solver = new SatSolverBuilder(encoder.variables()).build();

        return samePolarity
                ? samePolarityDisagreement(encoder, solver, contextScope, someAMatches, someBMatches, contextRulesA, contextRulesB)
                : oppositePolarityDisagreement(encoder, solver, contextScope, someAMatches, someBMatches, contextRulesA, contextRulesB);
    }

    private Optional<Disagreement> oppositePolarityDisagreement(final RuleSatEncoder encoder, final ISolver solver, final Rule.RuleScope contextScope, final int someAMatches, final int someBMatches, final List<Rule> contextRulesA, final List<Rule> contextRulesB) {
        if (isSatisfiable(new int[]{someAMatches, someBMatches}, solver)) {
            return Optional.of(new Disagreement(contextScope, Disagreement.Kind.BOTH_MATCH, encoder.decode(solver),
                    matchingRules(encoder, solver, contextRulesA), matchingRules(encoder, solver, contextRulesB)));
        }
        if (isSatisfiable(new int[]{-someAMatches, -someBMatches}, solver)) {
            return Optional.of(new Disagreement(contextScope, Disagreement.Kind.NEITHER_MATCHES, encoder.decode(solver),
                    matchingRules(encoder, solver, contextRulesA), matchingRules(encoder, solver, contextRulesB)));
        }
        return Optional.empty();
    }

    private Optional<Disagreement> samePolarityDisagreement(final RuleSatEncoder encoder, final ISolver solver, final Rule.RuleScope contextScope, final int someAMatches, final int someBMatches, final List<Rule> contextRulesA, final List<Rule> contextRulesB) {
        if (isSatisfiable(new int[]{someAMatches, -someBMatches}, solver)) {
            return Optional.of(new Disagreement(contextScope, Disagreement.Kind.ONLY_A_MATCHES, encoder.decode(solver),
                    matchingRules(encoder, solver, contextRulesA), matchingRules(encoder, solver, contextRulesB)));
        }
        if (isSatisfiable(new int[]{-someAMatches, someBMatches}, solver)) {
            return Optional.of(new Disagreement(contextScope, Disagreement.Kind.ONLY_B_MATCHES, encoder.decode(solver),
                    matchingRules(encoder, solver, contextRulesA), matchingRules(encoder, solver, contextRulesB)));
        }
        return Optional.empty();
    }

    private List<Rule> matchingRules(final RuleSatEncoder encoder, final ISolver solver, final List<Rule> rules) {
        return rules.stream()
                .filter(rule -> isTrue(solver, encoder.trueLiteralOf(rule)))
                .toList();
    }

    private boolean isTrue(final ISolver solver, final int literal) {
        return literal > 0 ? solver.model(literal) : !solver.model(-literal);
    }

    private int someRuleMatches(final RuleSatEncoder encoder, final List<Rule> rules) {
        final var variables = encoder.variables();
        final var some = variables.allocate();

        final var atLeastOne = new int[rules.size() + 1];
        var i = 0;
        for (final var rule : rules) {
            final var literal = encoder.trueLiteralOf(rule);
            variables.addClause(-literal, some);
            atLeastOne[i++] = literal;
        }
        atLeastOne[i] = -some;
        variables.addClause(atLeastOne);

        return some;
    }

    private boolean isSatisfiable(final int[] assumptions, final ISolver solver) {
        try {
            return solver.isSatisfiable(new VecInt(assumptions));
        } catch (final TimeoutException e) {
            return true;
        }
    }

    /**
     * A concrete classification where the two rule sets disagree.
     *
     * @param context        the context the disagreement was found in
     * @param kind           how the two rule sets disagreed
     * @param classification the disagreeing classification, per side; a dimension left out is unclassified (NULL) on that side
     * @param matchingRulesA the rules of {@code rulesA} that matched this classification
     * @param matchingRulesB the rules of {@code rulesB} that matched this classification
     */
    public record Disagreement(
            Rule.RuleScope context,
            Kind kind,
            Map<PartitionExpression.Relative.Side, Map<Dimension, Dimension.Partition>> classification,
            List<Rule> matchingRulesA,
            List<Rule> matchingRulesB
    ) {

        /**
         * Which way the two rule sets disagreed.
         */
        public enum Kind {
            /** Opposite {@code RuleStrategy} only: both rule sets matched at once, rulesA forbids it, rulesB independently allows it. */
            BOTH_MATCH,
            /** Opposite {@code RuleStrategy} only: neither rule set matched, rulesA's default applies, rulesB's opposite default applies. */
            NEITHER_MATCHES,
            /** Same {@code RuleStrategy} only: only rulesA matched, so only rulesA forbids/allows it (per the shared strategy). */
            ONLY_A_MATCHES,
            /** Same {@code RuleStrategy} only: only rulesB matched, so only rulesB forbids/allows it (per the shared strategy). */
            ONLY_B_MATCHES
        }
    }
}
