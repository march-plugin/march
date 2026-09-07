package io.github.march_plugin.core.config.rules.model.ast;

import io.github.march_plugin.core.config.rules.exceptions.NullComparisonException;

/**
 * Represents logical composition (AND, OR, NOT) in the rule AST.
 */
public sealed interface LogicalExpression {

    /**
     * Represents a logical AND operation.
     *
     * @param left  the left-hand side logical expression
     * @param right the right-hand side logical expression
     */
    record And(LogicalExpression left, LogicalExpression right) implements LogicalExpression {
        /**
         * Validates binary AND logic.
         *
         * @param left  the left-hand side logical expression
         * @param right the right-hand side logical expression
         */
        public And {
            if (left == null || right == null) {
                throw new NullComparisonException();
            }
        }

        @Override
        public String toString() {
            return wrapIfLowerPrecedence(left) + " AND " + wrapIfLowerPrecedence(right);
        }

        private static String wrapIfLowerPrecedence(final LogicalExpression expression) {
            return expression instanceof Or ? "(" + expression + ")" : expression.toString();
        }
    }

    /**
     * Represents a logical OR operation.
     *
     * @param left  the left-hand side logical expression
     * @param right the right-hand side logical expression
     */
    record Or(LogicalExpression left, LogicalExpression right) implements LogicalExpression {
        /**
         * Validates binary OR logic.
         *
         * @param left  the left-hand side logical expression
         * @param right the right-hand side logical expression
         */
        public Or {
            if (left == null || right == null) {
                throw new NullComparisonException();
            }
        }

        @Override
        public String toString() {
            return left.toString() + " OR " + right.toString();
        }
    }

    /**
     * Represents a logical NOT (!) operation.
     *
     * @param expression the logical expression to negate
     */
    record Not(LogicalExpression expression) implements LogicalExpression {
        /**
         * Validates unary NOT logic.
         *
         * @param expression the logical expression to negate
         */
        public Not {
            if (expression == null) {
                throw new NullComparisonException();
            }
        }

        @Override
        public String toString() {
            return "!" + wrapIfLowerPrecedence(expression);
        }

        private static String wrapIfLowerPrecedence(final LogicalExpression expression) {
            return (expression instanceof And || expression instanceof Or) ? "(" + expression + ")" : expression.toString();
        }
    }

    /**
     * Wraps a ComparisonExpression to be used within a logical tree.
     *
     * @param comparison the comparison expression to wrap
     */
    record ComparisonWrap(ComparisonExpression comparison) implements LogicalExpression {
        /**
         * Validates the wrapped comparison.
         *
         * @param comparison the comparison expression to wrap
         */
        public ComparisonWrap {
            if (comparison == null) {
                throw new NullComparisonException();
            }
        }

        @Override
        public String toString() {
            return comparison.toString();
        }
    }

    /**
     * The result of reducing an expression that is now known to always hold, regardless of any dimension
     * still left unresolved.
     */
    record AlwaysTrue() implements LogicalExpression {
        @Override
        public String toString() {
            return "TRUE";
        }
    }

    /**
     * The result of reducing an expression that is now known to never hold, regardless of any dimension
     * still left unresolved.
     */
    record AlwaysFalse() implements LogicalExpression {
        @Override
        public String toString() {
            return "FALSE";
        }
    }
}
