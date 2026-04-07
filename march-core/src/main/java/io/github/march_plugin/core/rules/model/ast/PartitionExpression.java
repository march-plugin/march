package io.github.march_plugin.core.rules.model.ast;

import io.github.march_plugin.core.dimensions.model.Dimension;

public sealed interface PartitionExpression {

    record Fixed(Dimension.Partition partition) implements PartitionExpression {
        @Override
        public String toString() {
            return partition.getDimension().getName() + "." + partition.getName();
        }
    }

    record Relative(Side side, Dimension dimension) implements PartitionExpression {
        @Override
        public String toString() {
            return side.toString().toLowerCase() + "." + dimension.getName();
        }

        public enum Side {SOURCE, TARGET}
    }

    record Null() implements PartitionExpression {
        @Override
        public String toString() {
            return "NULL";
        }
    }
}