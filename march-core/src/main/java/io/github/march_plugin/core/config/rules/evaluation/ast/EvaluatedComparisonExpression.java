package io.github.march_plugin.core.config.rules.evaluation.ast;

import java.util.HashSet;
import java.util.List;

public sealed interface EvaluatedComparisonExpression {

    record Equal(EvaluatedPartitionExpression left, EvaluatedPartitionExpression right)
            implements EvaluatedComparisonExpression {
        @Override
        public String toString() {
            return left.toString() + " == " + right.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Equal other) {
                return left.equals(other.left) && right.equals(other.right);
            }
            return false;
        }
    }

    record NotEqual(EvaluatedPartitionExpression left, EvaluatedPartitionExpression right)
            implements EvaluatedComparisonExpression {
        @Override
        public String toString() {
            return left.toString() + " != " + right.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof NotEqual other) {
                return left.equals(other.left) && right.equals(other.right);
            }
            return false;
        }
    }

    record In(EvaluatedPartitionExpression left, List<EvaluatedPartitionExpression> rights)
            implements EvaluatedComparisonExpression {
        @Override
        public String toString() {
            final var sb = new StringBuilder();
            sb.append(left.toString());
            sb.append(" IN (");

            for (var x = 0; x < rights.size() - 1; x++) {
                sb.append(rights.get(x).toString()).append("|");
            }
            sb.append(rights.getLast().toString());
            sb.append(")");
            return sb.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof In other) {
                return left.equals(other.left) && new HashSet<>(rights).equals(new HashSet<>(other.rights));
            }
            return false;
        }
    }
}