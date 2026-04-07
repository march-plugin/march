package io.github.march_plugin.core.rules.model.ast;

import io.github.march_plugin.core.dimensions.model.Dimension;
import io.github.march_plugin.core.rules.exceptions.ConstantComparisonException;
import io.github.march_plugin.core.rules.exceptions.DimensionMismatchException;
import io.github.march_plugin.core.rules.exceptions.DuplicatePartitionException;
import io.github.march_plugin.core.rules.exceptions.NullComparisonException;
import io.github.march_plugin.core.rules.exceptions.RedundantComparisonException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents comparison logic between partitions in the rule AST.
 */
public sealed interface ComparisonExpression {

    /**
     * Represents an equality check (==).
     *
     * @param left  the left-hand side partition expression
     * @param right the right-hand side partition expression
     */
    record Equal(PartitionExpression left, PartitionExpression right) implements ComparisonExpression {
        /**
         * Validates Equal comparison.
         *
         * @param left  the left-hand side partition expression
         * @param right the right-hand side partition expression
         **/
        public Equal {
            ComparisonExpression.validate(left, right);
        }

        @Override
        public String toString() {
            return left.toString() + " == " + right.toString();
        }
    }

    /**
     * Represents an inequality check (!=).
     *
     * @param left  the left-hand side partition expression
     * @param right the right-hand side partition expression
     */
    record NotEqual(PartitionExpression left, PartitionExpression right) implements ComparisonExpression {
        /**
         * Validates NotEqual comparison.
         *
         * @param left  the left-hand side partition expression
         * @param right the right-hand side partition expression
         **/
        public NotEqual {
            ComparisonExpression.validate(left, right);
        }

        @Override
        public String toString() {
            return left.toString() + " != " + right.toString();
        }
    }

    /**
     * Represents a set-membership check (IN).
     *
     * @param left   the relative partition being checked
     * @param rights the list of fixed partitions to check against
     */
    record In(PartitionExpression.Relative left, List<PartitionExpression.Fixed> rights) implements ComparisonExpression {
        /**
         * Validates In comparison.
         *
         * @param left   the relative partition being checked
         * @param rights the list of fixed partitions to check against
         **/
        public In {
            if (left == null || rights == null || rights.isEmpty()) {
                throw new NullComparisonException();
            }

            final Set<Dimension.Partition> seen = new HashSet<>();
            for (final var fixed : rights) {
                if (!seen.add(fixed.partition())) {
                    throw new DuplicatePartitionException(fixed.partition().getName());
                }

                if (!fixed.partition().getDimension().equals(left.dimension())) {
                    throw new DimensionMismatchException(left.dimension().getName(), fixed.partition().getDimension().getName());
                }
            }
        }

        @Override
        public String toString() {
            final var partitions = rights.stream()
                    .map(f -> f.partition().getName())
                    .collect(java.util.stream.Collectors.joining("|"));

            return "%s IN %s.(%s)".formatted(
                    left.toString(),
                    left.dimension().getName(),
                    partitions
            );
        }
    }

    /**
     * Validates that a comparison is semantically correct.
     */
    private static void validate(final PartitionExpression left, final PartitionExpression right) {
        if (left == null || right == null) {
            throw new NullComparisonException();
        }

        if (!(left instanceof PartitionExpression.Relative) && !(right instanceof PartitionExpression.Relative)) {
            throw new ConstantComparisonException();
        }

        if (Objects.equals(left, right)) {
            throw new RedundantComparisonException(left + " == " + right);
        }

        final var lDim = getDim(left);
        final var rDim = getDim(right);

        if (lDim != null && rDim != null && !lDim.equals(rDim)) {
            throw new DimensionMismatchException(lDim.getName(), rDim.getName());
        }
    }

    private static Dimension getDim(final PartitionExpression expr) {
        if (expr instanceof PartitionExpression.Relative rel) {
            return rel.dimension();
        }
        if (expr instanceof PartitionExpression.Fixed fix) {
            return fix.partition().getDimension();
        }
        return null;
    }
}