package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedComparisonExpression;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedPartitionExpression;
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
    public EvaluatedLogicalExpression reduce(final LogicalExpression expression, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        return switch (expression) {
            case LogicalExpression.And and -> reduceAnd(and, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.Or or -> reduceOr(or, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.ComparisonWrap wrap -> reduceComparison(wrap.comparison(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case LogicalExpression.Not not -> reduceNot(not, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        };
    }

    private EvaluatedLogicalExpression reduceAnd(final LogicalExpression.And and, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var left = reduce(and.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var right = reduce(and.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (left instanceof EvaluatedLogicalExpression.AlwaysFalse || right instanceof EvaluatedLogicalExpression.AlwaysFalse) {
            return new EvaluatedLogicalExpression.AlwaysFalse();
        }
        if (left instanceof EvaluatedLogicalExpression.AlwaysTrue) {
            return right;
        }
        if (right instanceof EvaluatedLogicalExpression.AlwaysTrue) {
            return left;
        }
        return new EvaluatedLogicalExpression.And(left, right);
    }

    private EvaluatedLogicalExpression reduceOr(final LogicalExpression.Or or, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var left = reduce(or.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var right = reduce(or.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (left instanceof EvaluatedLogicalExpression.AlwaysTrue || right instanceof EvaluatedLogicalExpression.AlwaysTrue) {
            return new EvaluatedLogicalExpression.AlwaysTrue();
        }
        if (left instanceof EvaluatedLogicalExpression.AlwaysFalse) {
            return right;
        }
        if (right instanceof EvaluatedLogicalExpression.AlwaysFalse) {
            return left;
        }
        return new EvaluatedLogicalExpression.Or(left, right);
    }

    private EvaluatedLogicalExpression reduceComparison(final ComparisonExpression comp, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        return switch (comp) {
            case ComparisonExpression.Equal eq -> reduceEqual(eq, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case ComparisonExpression.NotEqual ne -> reduceNotEqual(ne, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
            case ComparisonExpression.In inExpr -> reduceIn(inExpr, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        };
    }

    private EvaluatedLogicalExpression reduceNot(final LogicalExpression.Not not, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var inner = reduce(not.expression(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (inner instanceof EvaluatedLogicalExpression.AlwaysTrue) {
            return new EvaluatedLogicalExpression.AlwaysFalse();
        }
        if (inner instanceof EvaluatedLogicalExpression.AlwaysFalse) {
            return new EvaluatedLogicalExpression.AlwaysTrue();
        }

        return new EvaluatedLogicalExpression.Not(inner);
    }

    private EvaluatedLogicalExpression reduceEqual(final ComparisonExpression.Equal eq, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var leftRes = resolvePartially(eq.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var rightRes = resolvePartially(eq.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (leftRes instanceof Resolved l && rightRes instanceof Resolved r) {
            return Objects.equals(l.partition(), r.partition()) ?
                    new EvaluatedLogicalExpression.AlwaysTrue() : new EvaluatedLogicalExpression.AlwaysFalse();
        }
        return new EvaluatedLogicalExpression.ComparisonWrap(
                new EvaluatedComparisonExpression.Equal(toEvaluated(eq.left()), toEvaluated(eq.right()))
        );
    }

    private EvaluatedLogicalExpression reduceNotEqual(final ComparisonExpression.NotEqual ne, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var leftRes = resolvePartially(ne.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var rightRes = resolvePartially(ne.right(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);

        if (leftRes instanceof Resolved l && rightRes instanceof Resolved r) {
            return !Objects.equals(l.partition(), r.partition()) ?
                    new EvaluatedLogicalExpression.AlwaysTrue() : new EvaluatedLogicalExpression.AlwaysFalse();
        }
        return new EvaluatedLogicalExpression.ComparisonWrap(
                new EvaluatedComparisonExpression.NotEqual(toEvaluated(ne.left()), toEvaluated(ne.right()))
        );
    }

    private EvaluatedLogicalExpression reduceIn(final ComparisonExpression.In inExpr, final Set<Dimension.Partition> sourcePartitions, final Set<Dimension.Partition> targetPartitions, final Map<Dimension, Dimension.Partition> sourceForcedValues, final Map<Dimension, Dimension.Partition> targetForcedValues) {
        final var leftRes = resolvePartially(inExpr.left(), sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues);
        final var rightResults = inExpr.rights().stream()
                .map(r -> resolvePartially(r, sourcePartitions, targetPartitions, sourceForcedValues, targetForcedValues))
                .toList();

        if (leftRes instanceof Resolved l && rightResults.stream().allMatch(r -> r instanceof Resolved)) {
            final var match = rightResults.stream()
                    .map(r -> ((Resolved) r).partition())
                    .anyMatch(p -> Objects.equals(l.partition(), p));
            return match ? new EvaluatedLogicalExpression.AlwaysTrue() : new EvaluatedLogicalExpression.AlwaysFalse();
        }

        return new EvaluatedLogicalExpression.ComparisonWrap(
                new EvaluatedComparisonExpression.In(
                        toEvaluated(inExpr.left()),
                        inExpr.rights().stream().map(this::toEvaluated).toList()
                )
        );
    }

    private EvaluatedPartitionExpression toEvaluated(final PartitionExpression expr) {
        return switch (expr) {
            case PartitionExpression.Fixed f -> new EvaluatedPartitionExpression.Fixed(f.partition());
            case PartitionExpression.Relative r -> new EvaluatedPartitionExpression.Relative(
                    EvaluatedPartitionExpression.Relative.Side.valueOf(r.side().name()), r.dimension());
            case PartitionExpression.Null n -> new EvaluatedPartitionExpression.Null();
        };
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