package io.github.march_plugin.core.config.rules.parser;

import io.github.march_plugin.core.config.rules.exceptions.RedundantComparisonException;
import io.github.march_plugin.core.config.rules.exceptions.RedundantLogicalOperationException;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;

import java.util.Objects;

/**
 * Rejects a freshly parsed rule whose author almost certainly made a mistake:
 * an AND/OR combining two identical operands, or an == / != comparing a side against itself.
 */
class RuleRedundancyValidator {

    /**
     * Validates a freshly parsed rule tree.
     *
     * @param expression the root of the parsed rule tree
     * @throws RedundantLogicalOperationException if an AND/OR combines two identical operands
     * @throws RedundantComparisonException if an == / != compares a side against itself
     */
    void validate(final LogicalExpression expression) {
        switch (expression) {
            case LogicalExpression.And and -> {
                requireDistinctOperands(and.left(), and.right());
                validate(and.left());
                validate(and.right());
            }
            case LogicalExpression.Or or -> {
                requireDistinctOperands(or.left(), or.right());
                validate(or.left());
                validate(or.right());
            }
            case LogicalExpression.Not not -> validate(not.expression());
            case LogicalExpression.ComparisonWrap wrap -> validateComparison(wrap.comparison());
            case LogicalExpression.AlwaysTrue ignored -> {
            }
            case LogicalExpression.AlwaysFalse ignored -> {
            }
        }
    }

    private void validateComparison(final ComparisonExpression comparison) {
        switch (comparison) {
            case ComparisonExpression.Equal eq -> requireDistinctSides(eq.left(), eq.right());
            case ComparisonExpression.NotEqual ne -> requireDistinctSides(ne.left(), ne.right());
            case ComparisonExpression.In ignored -> {
                // IN already guarantees non-duplicate, dimension-matching partitions in its constructor.
            }
        }
    }

    private void requireDistinctOperands(final LogicalExpression left, final LogicalExpression right) {
        if (Objects.equals(left, right)) {
            throw new RedundantLogicalOperationException(left.toString());
        }
    }

    private void requireDistinctSides(final PartitionExpression left, final PartitionExpression right) {
        if (Objects.equals(left, right)) {
            throw new RedundantComparisonException(left + " == " + right);
        }
    }
}
