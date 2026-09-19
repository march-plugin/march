package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.sat4j.specs.ISolver;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
     * @param moduleContext        {@code true} to restrict the classification domain to combinations at module level
     * @param dependencyConfig     whether the classification domain is further restricted to leaves only
     */
    public RuleSatEncoder(final List<Rule> rules, final ModuleModularity projectStructureRoot, final boolean moduleContext, final DependencyConfig dependencyConfig) {
        final var dimensions = RuleDimensionCollector.referencedDimensions(rules).stream().sorted().toList();

        for (final var side : PartitionExpression.Relative.Side.values()) {
            noneVars.put(side, new HashMap<>());
            partitionVars.put(side, new HashMap<>());
            for (final var dimension : dimensions) {
                encodeDomain(side, dimension);
            }
        }

        if (projectStructureRoot != null) {
            final var combinations = combinationsFor(projectStructureRoot, moduleContext, dependencyConfig);
            for (final var side : PartitionExpression.Relative.Side.values()) {
                encodeTreeRestriction(side, dimensions, combinations);
            }
        }

        encodeSelfDependencyExclusion(dimensions);

        final var expressionEncoder = new RuleExpressionEncoder(variables, noneVars, partitionVars);
        for (final var rule : rules) {
            final var pair = expressionEncoder.encode(rule.definition());
            ruleTrueLiterals.put(rule, pair.t());
        }
    }

    private static List<Map<Dimension, Dimension.Partition>> combinationsFor(final ModuleModularity projectStructureRoot, final boolean moduleContext, final DependencyConfig dependencyConfig) {
        if (dependencyConfig == DependencyConfig.LEAVES_ONLY) {
            return moduleContext
                    ? ModuleCombinationFinder.findLeafModuleCombinations(projectStructureRoot)
                    : ModuleCombinationFinder.findLeafCombinations(projectStructureRoot);
        }
        return moduleContext
                ? ModuleCombinationFinder.findModuleCombinations(projectStructureRoot)
                : ModuleCombinationFinder.findCombinations(projectStructureRoot);
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

    /**
     * Gets the literal to assume for testing whether {@code rule} could ever fire on its own, regardless of
     * every other rule.
     *
     * @param rule the rule to assume {@code TRUE}
     * @return {@code rule}'s literal, as a single-element assumption array
     */
    public int[] assumptionsForReachability(final Rule rule) {
        return new int[]{trueLiteral(rule)};
    }

    /**
     * Gets the literal that is true iff {@code rule}'s condition is satisfied.
     *
     * @param rule the rule to get the literal of
     * @return {@code rule}'s true literal
     */
    public int trueLiteralOf(final Rule rule) {
        return trueLiteral(rule);
    }

    /**
     * Decodes {@code solver}'s last found model into the classification it represents, per side.
     *
     * @param solver a solver that just returned {@code true} from {@code isSatisfiable(...)}
     * @return for each side, every dimension classified to a partition in the model
     */
    public Map<PartitionExpression.Relative.Side, Map<Dimension, Dimension.Partition>> decode(final ISolver solver) {
        final var result = new EnumMap<PartitionExpression.Relative.Side, Map<Dimension, Dimension.Partition>>(PartitionExpression.Relative.Side.class);
        for (final var side : PartitionExpression.Relative.Side.values()) {
            final var sideResult = new HashMap<Dimension, Dimension.Partition>();
            for (final var dimensionEntry : partitionVars.get(side).entrySet()) {
                for (final var partitionEntry : dimensionEntry.getValue().entrySet()) {
                    if (solver.model(partitionEntry.getValue())) {
                        sideResult.put(dimensionEntry.getKey(), partitionEntry.getKey());
                    }
                }
            }
            result.put(side, sideResult);
        }
        return result;
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

    private void encodeTreeRestriction(final PartitionExpression.Relative.Side side, final List<Dimension> dimensions, final List<Map<Dimension, Dimension.Partition>> combinations) {
        if (combinations.isEmpty()) {
            return;
        }

        final var treeDimensions = combinations.stream()
                .flatMap(combo -> combo.keySet().stream())
                .collect(Collectors.toSet());

        for (final var dimension : dimensions) {
            if (!treeDimensions.contains(dimension)) {
                variables.addClause(noneVars.get(side).get(dimension));
            }
        }

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

    private void encodeSelfDependencyExclusion(final List<Dimension> dimensions) {
        if (dimensions.isEmpty()) {
            return;
        }

        final var notAllEqual = new int[dimensions.size()];
        var i = 0;
        for (final var dimension : dimensions) {
            notAllEqual[i++] = -encodeDimensionEquality(dimension);
        }
        variables.addClause(notAllEqual);
    }

    private int encodeDimensionEquality(final Dimension dimension) {
        final var sourcePartitionVars = partitionVars.get(PartitionExpression.Relative.Side.SOURCE).get(dimension);
        final var targetPartitionVars = partitionVars.get(PartitionExpression.Relative.Side.TARGET).get(dimension);

        final var matchesPerValue = new ArrayList<Integer>();
        for (final var partition : dimension.getPartitions()) {
            matchesPerValue.add(encodeBothTrue(sourcePartitionVars.get(partition), targetPartitionVars.get(partition)));
        }
        matchesPerValue.add(encodeBothTrue(
                noneVars.get(PartitionExpression.Relative.Side.SOURCE).get(dimension),
                noneVars.get(PartitionExpression.Relative.Side.TARGET).get(dimension)));

        return encodeAnyTrue(matchesPerValue);
    }

    private int encodeBothTrue(final int a, final int b) {
        final var result = variables.allocate();
        variables.addClause(-result, a);
        variables.addClause(-result, b);
        variables.addClause(result, -a, -b);
        return result;
    }

    private int encodeAnyTrue(final List<Integer> literals) {
        final var result = variables.allocate();
        for (final var literal : literals) {
            variables.addClause(-literal, result);
        }
        final var atLeastOne = new int[literals.size() + 1];
        for (var i = 0; i < literals.size(); i++) {
            atLeastOne[i] = literals.get(i);
        }
        atLeastOne[literals.size()] = -result;
        variables.addClause(atLeastOne);
        return result;
    }
}
