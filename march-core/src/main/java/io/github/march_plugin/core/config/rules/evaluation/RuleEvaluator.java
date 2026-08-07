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

    /**
     * Evaluates if a Dependency matches a {@link LogicalExpression}.
     *
     * @param expression The logical expression to evaluate.
     * @param sourcePartitions The classified partitions of source.
     * @param targetPartitions The classified partitions of target.
     * @return if the dependency matches the expression
     */
    public boolean evaluate(final LogicalExpression expression, final Classification sourcePartitions, final Classification targetPartitions) {
        return switch (expression) {
            case LogicalExpression.And and -> evaluate(and.left(), sourcePartitions, targetPartitions) && evaluate(and.right(), sourcePartitions, targetPartitions);
            case LogicalExpression.Or or -> evaluate(or.left(), sourcePartitions, targetPartitions) || evaluate(or.right(), sourcePartitions, targetPartitions);
            case LogicalExpression.ComparisonWrap wrap -> evaluateComparison(wrap.comparison(), sourcePartitions, targetPartitions);
            case LogicalExpression.Not not -> !evaluate(not.expression(), sourcePartitions, targetPartitions);
        };
    }

    private boolean evaluateComparison(final ComparisonExpression comp, final Classification sourcePartitions, final Classification targetPartitions) {
        return switch (comp) {
            case ComparisonExpression.Equal eq -> {
                final var left = resolve(eq.left(), sourcePartitions, targetPartitions);
                final var right = resolve(eq.right(), sourcePartitions, targetPartitions);
                yield Objects.equals(left, right);
            }
            case ComparisonExpression.NotEqual ne -> {
                final var left = resolve(ne.left(), sourcePartitions, targetPartitions);
                final var right = resolve(ne.right(), sourcePartitions, targetPartitions);
                yield !Objects.equals(left, right);
            }
            case ComparisonExpression.In inExpr -> {
                final var left = resolve(inExpr.left(), sourcePartitions, targetPartitions);
                if (left == null) {
                    yield false;
                }
                yield inExpr.rights().stream().map(option -> resolve(option, sourcePartitions, targetPartitions)).filter(Objects::nonNull).anyMatch(allowedPartition -> Objects.equals(left, allowedPartition));
            }
        };
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