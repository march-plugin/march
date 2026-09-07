package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reduces a rule for a partial classification.
 */
public class RuleReducer {

    /**
     * Reduces a rule for a partition classification.
     *
     * @param expression The logical expression to evaluate.
     * @param sourcePartitions The partially classified partitions of source.
     * @param targetPartitions The partially classified partitions of target.
     * @param sourceForcedValues Dimensions of source that are, though not directly given, structurally forced to one specific value
     * @param targetForcedValues Dimensions of target that are, though not directly given, structurally forced to one specific value
     * @return The missing classifications to match the rule
     */
    public LogicalExpression reduce(final LogicalExpression expression, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        return switch (expression) {
            case LogicalExpression.And and -> reduceAnd(and, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.Or or -> reduceOr(or, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.ComparisonWrap wrap -> reduceComparison(wrap.comparison(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.Not not -> reduceNot(not, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.AlwaysTrue alwaysTrue -> alwaysTrue;
            case LogicalExpression.AlwaysFalse alwaysFalse -> alwaysFalse;
        };
    }

    private LogicalExpression reduceAnd(final LogicalExpression.And and, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var left = reduce(and.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var right = reduce(and.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (left instanceof LogicalExpression.AlwaysFalse || right instanceof LogicalExpression.AlwaysFalse) {
            return new LogicalExpression.AlwaysFalse();
        }
        if (left instanceof LogicalExpression.AlwaysTrue) {
            return right;
        }
        if (right instanceof LogicalExpression.AlwaysTrue) {
            return left;
        }
        return new LogicalExpression.And(left, right);
    }

    private LogicalExpression reduceOr(final LogicalExpression.Or or, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var left = reduce(or.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var right = reduce(or.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (left instanceof LogicalExpression.AlwaysTrue || right instanceof LogicalExpression.AlwaysTrue) {
            return new LogicalExpression.AlwaysTrue();
        }
        if (left instanceof LogicalExpression.AlwaysFalse) {
            return right;
        }
        if (right instanceof LogicalExpression.AlwaysFalse) {
            return left;
        }
        return new LogicalExpression.Or(left, right);
    }

    private LogicalExpression reduceComparison(final ComparisonExpression comp, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        return switch (comp) {
            case ComparisonExpression.Equal eq -> reduceEqual(eq, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case ComparisonExpression.NotEqual ne -> reduceNotEqual(ne, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case ComparisonExpression.In inExpr -> reduceIn(inExpr, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        };
    }

    private LogicalExpression reduceNot(final LogicalExpression.Not not, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var inner = reduce(not.expression(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (inner instanceof LogicalExpression.AlwaysTrue) {
            return new LogicalExpression.AlwaysFalse();
        }
        if (inner instanceof LogicalExpression.AlwaysFalse) {
            return new LogicalExpression.AlwaysTrue();
        }

        return new LogicalExpression.Not(inner);
    }

    private LogicalExpression reduceEqual(final ComparisonExpression.Equal eq, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var leftRes = resolvePartially(eq.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var rightRes = resolvePartially(eq.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (leftRes instanceof Resolved l && rightRes instanceof Resolved r) {
            return Objects.equals(l.partition(), r.partition()) ?
                    new LogicalExpression.AlwaysTrue() : new LogicalExpression.AlwaysFalse();
        }
        return new LogicalExpression.ComparisonWrap(eq);
    }

    private LogicalExpression reduceNotEqual(final ComparisonExpression.NotEqual ne, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var leftRes = resolvePartially(ne.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var rightRes = resolvePartially(ne.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (leftRes instanceof Resolved l && rightRes instanceof Resolved r) {
            return !Objects.equals(l.partition(), r.partition()) ?
                    new LogicalExpression.AlwaysTrue() : new LogicalExpression.AlwaysFalse();
        }
        return new LogicalExpression.ComparisonWrap(ne);
    }

    private LogicalExpression reduceIn(final ComparisonExpression.In inExpr, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var leftRes = resolvePartially(inExpr.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (leftRes instanceof Resolved l) {
            final var match = inExpr.rights().stream().anyMatch(fixed -> Objects.equals(l.partition(), fixed.partition()));
            return match ? new LogicalExpression.AlwaysTrue() : new LogicalExpression.AlwaysFalse();
        }

        return new LogicalExpression.ComparisonWrap(inExpr);
    }

    private PartialPartition resolvePartially(final PartitionExpression expr, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        return switch (expr) {
            case PartitionExpression.Fixed fix -> new Resolved(fix.partition());
            case PartitionExpression.Relative rel -> {
                final var active = (rel.side() == PartitionExpression.Relative.Side.SOURCE) ? sourcePartitions : targetPartitions;
                final var found = active.stream().filter(x -> x.getDimension().equals(rel.dimension())).findFirst().orElse(null);

                if (found != null) {
                    yield new Resolved(found);
                }

                final var forcedValues = (rel.side() == PartitionExpression.Relative.Side.SOURCE) ? sourceForcedValues : targetForcedValues;

                if (forcedValues.containsKey(rel.dimension())) {
                    yield new Resolved(forcedValues.get(rel.dimension()));
                }

                yield new Unknown();
            }
            case PartitionExpression.Null n -> new Resolved(null);
        };
    }

    private sealed interface PartialPartition {
    }

    private record Resolved(Dimension.Partition partition) implements PartialPartition {
    }

    private record Unknown() implements PartialPartition {
    }
}
