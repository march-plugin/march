package io.github.march_plugin.core.config.rules.evaluation.ast;



public sealed interface EvaluatedLogicalExpression {

    record And(EvaluatedLogicalExpression left, EvaluatedLogicalExpression right) implements EvaluatedLogicalExpression {
        @Override
        public String toString() {
            return wrapIfLowerPrecedence(left) + " && " + wrapIfLowerPrecedence(right);
        }

        private static String wrapIfLowerPrecedence(final EvaluatedLogicalExpression expression) {
            return expression instanceof Or ? "(" + expression + ")" : expression.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof And other) {
                return left.equals(other.left) && right.equals(other.right);
            }
            return false;
        }
    }

    record Or(EvaluatedLogicalExpression left, EvaluatedLogicalExpression right) implements EvaluatedLogicalExpression {
        @Override
        public String toString() {
            return left.toString() + " || " + right.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Or other) {
                return left.equals(other.left) && right.equals(other.right);
            }
            return false;
        }
    }

    record ComparisonWrap(EvaluatedComparisonExpression comparison) implements EvaluatedLogicalExpression {
        @Override
        public String toString() {
            return comparison.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ComparisonWrap other) {
                return comparison.equals(other.comparison);
            }
            return false;
        }
    }

    record Not(EvaluatedLogicalExpression expression) implements EvaluatedLogicalExpression {
        @Override
        public String toString() {
            return "!" + wrapIfLowerPrecedence(expression);
        }

        private static String wrapIfLowerPrecedence(final EvaluatedLogicalExpression expression) {
            return (expression instanceof And || expression instanceof Or) ? "(" + expression + ")" : expression.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Not other) {
                return expression.equals(other.expression);
            }
            return false;
        }
    }

    record AlwaysTrue() implements EvaluatedLogicalExpression {
        @Override
        public String toString() {
            return "TRUE";
        }
    }

    record AlwaysFalse() implements EvaluatedLogicalExpression {
        @Override
        public String toString() {
            return "FALSE";
        }
    }
}