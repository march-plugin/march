package io.github.march_plugin.core.config.rules.evaluation.ast;

import io.github.march_plugin.core.config.dimensions.model.Dimension;

public sealed interface EvaluatedPartitionExpression {

    record Fixed(Dimension.Partition partition) implements EvaluatedPartitionExpression {
        @Override
        public String toString() {
            return partition.getName();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Fixed other) {
                return partition.equals(other.partition);
            }
            return false;
        }
    }

    record Relative(Side side, Dimension dimension) implements EvaluatedPartitionExpression {
        @Override
        public String toString() {
            return side.toString().toLowerCase() + "." + dimension.getName();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Relative other) {
                return side.equals(other.side) && (dimension.equals(other.dimension));
            }
            return false;
        }

        public enum Side {SOURCE, TARGET}
    }

    record Null() implements EvaluatedPartitionExpression {
        @Override
        public String toString() {
            return "Null";
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Null;
        }
    }
}