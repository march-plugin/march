package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;

import java.util.ArrayList;
import java.util.Map;

/**
 * Encodes a rule's {@link LogicalExpression} AST into a pair of SAT literals: one that is true
 * iff the expression holds for a classification, and one that is true iff it definitely doesn't.
 */
final class RuleExpressionEncoder {

    private final SatVariablePool variables;
    private final Map<PartitionExpression.Relative.Side, Map<Dimension, Integer>> noneVars;
    private final Map<PartitionExpression.Relative.Side, Map<Dimension, Map<Dimension.Partition, Integer>>> partitionVars;

    /**
     * Creates an encoder over the given, already-encoded classification domain.
     *
     * @param variables     the pool to allocate SAT variables from and add CNF clauses to
     * @param noneVars      the "unclassified" literal for each side and dimension, from the domain encoding
     * @param partitionVars the literal for each side, dimension and partition, from the domain encoding
     */
    RuleExpressionEncoder(final SatVariablePool variables,
                           final Map<PartitionExpression.Relative.Side, Map<Dimension, Integer>> noneVars,
                           final Map<PartitionExpression.Relative.Side, Map<Dimension, Map<Dimension.Partition, Integer>>> partitionVars) {
        this.variables = variables;
        this.noneVars = noneVars;
        this.partitionVars = partitionVars;
    }

    /**
     * Encodes the given expression.
     *
     * @param expression the expression to encode
     * @return the expression's true/false literal pair
     */
    LitPair encode(final LogicalExpression expression) {
        return switch (expression) {
            case LogicalExpression.And and -> encodeAnd(encode(and.left()), encode(and.right()));
            case LogicalExpression.Or or -> encodeOr(encode(or.left()), encode(or.right()));
            case LogicalExpression.Not not -> encode(not.expression()).negate();
            case LogicalExpression.ComparisonWrap wrap -> encodeComparison(wrap.comparison());
            case LogicalExpression.AlwaysTrue ignored -> constant(true);
            case LogicalExpression.AlwaysFalse ignored -> constant(false);
        };
    }

    private LitPair encodeAnd(final LitPair left, final LitPair right) {
        final var t = variables.allocate();
        variables.addClause(-t, left.t());
        variables.addClause(-t, right.t());
        variables.addClause(t, -left.t(), -right.t());

        final var f = variables.allocate();
        variables.addClause(-left.f(), f);
        variables.addClause(-right.f(), f);
        variables.addClause(-f, left.f(), right.f());

        return new LitPair(t, f);
    }

    private LitPair encodeOr(final LitPair left, final LitPair right) {
        final var t = variables.allocate();
        variables.addClause(-left.t(), t);
        variables.addClause(-right.t(), t);
        variables.addClause(-t, left.t(), right.t());

        final var f = variables.allocate();
        variables.addClause(-f, left.f());
        variables.addClause(-f, right.f());
        variables.addClause(f, -left.f(), -right.f());

        return new LitPair(t, f);
    }

    private LitPair encodeComparison(final ComparisonExpression comparison) {
        return switch (comparison) {
            case ComparisonExpression.Equal eq -> encodeEqual(eq.left(), eq.right());
            case ComparisonExpression.NotEqual ne -> encodeEqual(ne.left(), ne.right()).negate();
            case ComparisonExpression.In in -> encodeIn(in);
        };
    }

    private LitPair encodeEqual(final PartitionExpression left, final PartitionExpression right) {
        if (left instanceof PartitionExpression.Null) {
            return encodeNullCheck((PartitionExpression.Relative) right);
        }
        if (right instanceof PartitionExpression.Null) {
            return encodeNullCheck((PartitionExpression.Relative) left);
        }
        if (left instanceof PartitionExpression.Relative rel && right instanceof PartitionExpression.Fixed fixed) {
            return encodeRelativeEqualsFixed(rel, fixed.partition());
        }
        if (right instanceof PartitionExpression.Relative rel && left instanceof PartitionExpression.Fixed fixed) {
            return encodeRelativeEqualsFixed(rel, fixed.partition());
        }
        // Both sides are Relative. ComparisonExpression's own validation guarantees they share a dimension.
        return encodeRelativeEqualsRelative((PartitionExpression.Relative) left, (PartitionExpression.Relative) right);
    }

    private LitPair encodeNullCheck(final PartitionExpression.Relative relative) {
        final var none = noneVars.get(relative.side()).get(relative.dimension());
        return new LitPair(none, -none);
    }

    private LitPair encodeRelativeEqualsFixed(final PartitionExpression.Relative relative, final Dimension.Partition partition) {
        final var none = noneVars.get(relative.side()).get(relative.dimension());
        final var trueLit = partitionVars.get(relative.side()).get(relative.dimension()).get(partition);

        final var f = variables.allocate();
        variables.addClause(-f, -none);
        variables.addClause(-f, -trueLit);
        variables.addClause(f, none, trueLit);

        return new LitPair(trueLit, f);
    }

    private LitPair encodeRelativeEqualsRelative(final PartitionExpression.Relative left, final PartitionExpression.Relative right) {
        final var dimension = left.dimension();
        final var leftVars = partitionVars.get(left.side()).get(dimension);
        final var rightVars = partitionVars.get(right.side()).get(dimension);

        final var matchLiterals = new ArrayList<Integer>();
        for (final var partition : dimension.getPartitions()) {
            final var leftLit = leftVars.get(partition);
            final var rightLit = rightVars.get(partition);
            final var match = variables.allocate();
            variables.addClause(-match, leftLit);
            variables.addClause(-match, rightLit);
            variables.addClause(match, -leftLit, -rightLit);
            matchLiterals.add(match);
        }

        final var t = variables.allocate();
        final var atLeastOneMatch = new int[matchLiterals.size() + 1];
        atLeastOneMatch[0] = -t;
        for (var i = 0; i < matchLiterals.size(); i++) {
            variables.addClause(-matchLiterals.get(i), t);
            atLeastOneMatch[i + 1] = matchLiterals.get(i);
        }
        variables.addClause(atLeastOneMatch);

        final var leftNone = noneVars.get(left.side()).get(dimension);
        final var rightNone = noneVars.get(right.side()).get(dimension);
        final var f = variables.allocate();
        variables.addClause(-f, -leftNone);
        variables.addClause(-f, -rightNone);
        variables.addClause(-f, -t);
        variables.addClause(f, leftNone, rightNone, t);

        return new LitPair(t, f);
    }

    private LitPair encodeIn(final ComparisonExpression.In in) {
        final var relative = in.left();
        final var none = noneVars.get(relative.side()).get(relative.dimension());
        final var vars = partitionVars.get(relative.side()).get(relative.dimension());

        final var optionLiterals = in.rights().stream()
                .map(fixed -> vars.get(fixed.partition()))
                .toList();

        final var t = variables.allocate();
        final var atLeastOne = new int[optionLiterals.size() + 1];
        atLeastOne[0] = -t;
        for (var i = 0; i < optionLiterals.size(); i++) {
            variables.addClause(-optionLiterals.get(i), t);
            atLeastOne[i + 1] = optionLiterals.get(i);
        }
        variables.addClause(atLeastOne);

        final var f = variables.allocate();
        variables.addClause(-f, -none);
        variables.addClause(-f, -t);
        variables.addClause(f, none, t);

        return new LitPair(t, f);
    }

    private LitPair constant(final boolean value) {
        final var v = variables.allocate();
        variables.addClause(value ? v : -v);
        return value ? new LitPair(v, -v) : new LitPair(-v, v);
    }

    record LitPair(int t, int f) {
        LitPair negate() {
            return new LitPair(f, t);
        }
    }
}
