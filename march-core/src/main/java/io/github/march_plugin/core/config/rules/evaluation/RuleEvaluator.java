package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;

import java.util.Objects;

/**
 * Evaluates if a dependency matches a rule.
 */
public class RuleEvaluator {

    private enum Trilean {
        TRUE, FALSE, UNKNOWN
    }

    /**
     * Evaluates if a Dependency matches a {@link LogicalExpression}.
     *
     * @param expression The logical expression to evaluate.
     * @param sourcePartitions The classified partitions of source.
     * @param targetPartitions The classified partitions of target.
     * @return if the dependency matches the expression
     */
    public boolean evaluate(final LogicalExpression expression, final Classification sourcePartitions, final Classification targetPartitions) {
        return evaluateTrilean(expression, sourcePartitions, targetPartitions) == Trilean.TRUE;
    }

    private Trilean evaluateTrilean(final LogicalExpression expression, final Classification sourcePartitions, final Classification targetPartitions) {
        return switch (expression) {
            case LogicalExpression.And and -> and(evaluateTrilean(and.left(), sourcePartitions, targetPartitions), evaluateTrilean(and.right(), sourcePartitions, targetPartitions));
            case LogicalExpression.Or or -> or(evaluateTrilean(or.left(), sourcePartitions, targetPartitions), evaluateTrilean(or.right(), sourcePartitions, targetPartitions));
            case LogicalExpression.ComparisonWrap wrap -> evaluateComparison(wrap.comparison(), sourcePartitions, targetPartitions);
            case LogicalExpression.Not not -> not(evaluateTrilean(not.expression(), sourcePartitions, targetPartitions));

            // AlwaysTrue/AlwaysFalse only occur as RuleReducer output, never in a freshly parsed rule.
            case LogicalExpression.AlwaysTrue ignored -> Trilean.TRUE;
            case LogicalExpression.AlwaysFalse ignored -> Trilean.FALSE;
        };
    }

    private static Trilean and(final Trilean left, final Trilean right) {
        if (left == Trilean.FALSE || right == Trilean.FALSE) {
            return Trilean.FALSE;
        }
        if (left == Trilean.UNKNOWN || right == Trilean.UNKNOWN) {
            return Trilean.UNKNOWN;
        }
        return Trilean.TRUE;
    }

    private static Trilean or(final Trilean left, final Trilean right) {
        if (left == Trilean.TRUE || right == Trilean.TRUE) {
            return Trilean.TRUE;
        }
        if (left == Trilean.UNKNOWN || right == Trilean.UNKNOWN) {
            return Trilean.UNKNOWN;
        }
        return Trilean.FALSE;
    }

    private static Trilean not(final Trilean value) {
        return switch (value) {
            case TRUE -> Trilean.FALSE;
            case FALSE -> Trilean.TRUE;
            case UNKNOWN -> Trilean.UNKNOWN;
        };
    }

    private Trilean evaluateComparison(final ComparisonExpression comp, final Classification sourcePartitions, final Classification targetPartitions) {
        return switch (comp) {
            case ComparisonExpression.Equal eq -> {
                final var left = resolve(eq.left(), sourcePartitions, targetPartitions);
                final var right = resolve(eq.right(), sourcePartitions, targetPartitions);
                if (!isNullCheck(eq.left(), eq.right()) && (left == null || right == null)) {
                    yield Trilean.UNKNOWN;
                }
                yield Objects.equals(left, right) ? Trilean.TRUE : Trilean.FALSE;
            }
            case ComparisonExpression.NotEqual ne -> {
                final var left = resolve(ne.left(), sourcePartitions, targetPartitions);
                final var right = resolve(ne.right(), sourcePartitions, targetPartitions);
                if (!isNullCheck(ne.left(), ne.right()) && (left == null || right == null)) {
                    yield Trilean.UNKNOWN;
                }
                yield !Objects.equals(left, right) ? Trilean.TRUE : Trilean.FALSE;
            }
            case ComparisonExpression.In inExpr -> {
                final var left = resolve(inExpr.left(), sourcePartitions, targetPartitions);
                if (left == null) {
                    // inExpr.left() is always a Relative (never the NULL literal), so this is always genuine
                    // absence, never a deliberate null-check.
                    yield Trilean.UNKNOWN;
                }
                final var match = inExpr.rights().stream().map(option -> resolve(option, sourcePartitions, targetPartitions)).filter(Objects::nonNull).anyMatch(allowedPartition -> Objects.equals(left, allowedPartition));
                yield match ? Trilean.TRUE : Trilean.FALSE;
            }
        };
    }

    /**
     * Whether one side of a comparison is the explicit {@code NULL} literal.
     */
    private static boolean isNullCheck(final PartitionExpression left, final PartitionExpression right) {
        return left instanceof PartitionExpression.Null || right instanceof PartitionExpression.Null;
    }

    private Dimension.Partition resolve(final PartitionExpression expr, final Classification sourcePartitions, final Classification targetPartitions) {
        return switch (expr) {
            case PartitionExpression.Fixed fix -> fix.partition();
            case PartitionExpression.Relative rel -> {
                final var active = (rel.side() == PartitionExpression.Relative.Side.SOURCE) ? sourcePartitions : targetPartitions;

                yield active.getPartitions().stream().filter(x -> x.getDimension().equals(rel.dimension())).findFirst().orElse(null);
            }
            case PartitionExpression.Null n -> null;
        };
    }
}
