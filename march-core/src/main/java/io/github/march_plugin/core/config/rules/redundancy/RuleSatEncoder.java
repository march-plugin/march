package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encodes a set of rules, and the classification domain they reference, into a boolean satisfiability problem.
 */
final class RuleSatEncoder {

    private final SatVariablePool variables = new SatVariablePool();

    private final Map<PartitionExpression.Relative.Side, Map<Dimension, Integer>> noneVars = new EnumMap<>(PartitionExpression.Relative.Side.class);
    private final Map<PartitionExpression.Relative.Side, Map<Dimension, Map<Dimension.Partition, Integer>>> partitionVars = new EnumMap<>(PartitionExpression.Relative.Side.class);

    private final Map<Rule, Integer> ruleTrueLiterals = new IdentityHashMap<>();

    /**
     * Builds the SAT encoding for the given rules and possible classifications.
     *
     * @param rules                the rules to encode
     * @param projectStructureRoot the root of the project structure's module tree, or {@code null} to leave every dimension combination unrestricted
     */
    public RuleSatEncoder(final List<Rule> rules, final ModuleModularity projectStructureRoot) {
        final var dimensions = RuleDimensionCollector.referencedDimensions(rules);

        for (final var side : PartitionExpression.Relative.Side.values()) {
            noneVars.put(side, new HashMap<>());
            partitionVars.put(side, new HashMap<>());
            for (final var dimension : dimensions) {
                encodeDomain(side, dimension);
            }
        }

        if (projectStructureRoot != null) {
            final var combinations = ModuleCombinationFinder.findCombinations(projectStructureRoot);
            for (final var side : PartitionExpression.Relative.Side.values()) {
                encodeTreeRestriction(side, dimensions, combinations);
            }
        }

        final var expressionEncoder = new RuleExpressionEncoder(variables, noneVars, partitionVars);
        for (final var rule : rules) {
            final var pair = expressionEncoder.encode(rule.definition());
            ruleTrueLiterals.put(rule, pair.t());
        }
    }

    /**
     * Gets the literals to assume for testing whether {@code trueRule} could fire while every other rule in
     * {@code allRules} is excluded.
     *
     * @param trueRule the rule to assume {@code TRUE}
     * @param allRules the rules to assume {@code FALSE}, except {@code trueRule} itself
     * @return {@code trueRule}'s literal, followed by the negated literal of every other rule
     */
    public int[] assumptionsFor(final Rule trueRule, final List<Rule> allRules) {
        final var assumptions = new int[allRules.size()];
        assumptions[0] = trueLiteral(trueRule);
        var i = 1;
        for (final var other : allRules) {
            if (other != trueRule) {
                assumptions[i++] = -trueLiteral(other);
            }
        }
        return assumptions;
    }

    private int trueLiteral(final Rule rule) {
        return ruleTrueLiterals.get(rule);
    }

    /**
     * Gets the SAT variables and CNF clauses produced while encoding.
     *
     * @return the SAT variables and CNF clauses produced while encoding
     */
    public SatVariablePool variables() {
        return variables;
    }

    private void encodeDomain(final PartitionExpression.Relative.Side side, final Dimension dimension) {
        final var none = variables.allocate();
        noneVars.get(side).put(dimension, none);

        final var vars = new HashMap<Dimension.Partition, Integer>();
        for (final var partition : dimension.getPartitions()) {
            vars.put(partition, variables.allocate());
        }
        partitionVars.get(side).put(dimension, vars);

        // Every component is either unclassified along this dimension, or classified to exactly one partition.
        final var all = new ArrayList<>(vars.values());
        all.add(none);
        encodeExactlyOne(all);
    }

    // At-least-one clause plus a pairwise at-most-one clause per pair, forcing exactly one literal true.
    private void encodeExactlyOne(final List<Integer> literals) {
        variables.addClause(literals.stream().mapToInt(Integer::intValue).toArray());
        for (var a = 0; a < literals.size(); a++) {
            for (var b = a + 1; b < literals.size(); b++) {
                variables.addClause(-literals.get(a), -literals.get(b));
            }
        }
    }

    // Additive on top of encodeDomain: a dimension not covered by any combination stays unrestricted.
    private void encodeTreeRestriction(final PartitionExpression.Relative.Side side, final Set<Dimension> dimensions, final List<Map<Dimension, Dimension.Partition>> combinations) {
        if (combinations.isEmpty()) {
            return;
        }

        final var treeDimensions = combinations.stream()
                .flatMap(combo -> combo.keySet().stream())
                .collect(Collectors.toSet());
        final var coveredDimensions = dimensions.stream().filter(treeDimensions::contains).toList();
        if (coveredDimensions.isEmpty()) {
            return;
        }

        final var selectors = new ArrayList<Integer>();
        for (final var combination : combinations) {
            final var selector = variables.allocate();
            selectors.add(selector);
            for (final var dimension : coveredDimensions) {
                final var partition = combination.get(dimension);
                final var literal = partition != null
                        ? partitionVars.get(side).get(dimension).get(partition)
                        : noneVars.get(side).get(dimension);
                variables.addClause(-selector, literal);
            }
        }

        encodeExactlyOne(selectors);
    }
}
