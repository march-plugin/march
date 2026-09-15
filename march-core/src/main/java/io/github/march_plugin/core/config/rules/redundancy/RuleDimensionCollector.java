package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds every {@link Dimension} referenced anywhere in a set of rules.
 */
final class RuleDimensionCollector {

    private RuleDimensionCollector() {
    }

    /**
     * Finds every dimension referenced anywhere in the given rules.
     *
     * @param rules the rules to scan
     * @return the referenced dimensions
     */
    static Set<Dimension> referencedDimensions(final List<Rule> rules) {
        final var dimensions = new HashSet<Dimension>();
        for (final var rule : rules) {
            collectDimensions(rule.definition(), dimensions);
        }
        return dimensions;
    }

    private static void collectDimensions(final LogicalExpression expression, final Set<Dimension> dimensions) {
        switch (expression) {
            case LogicalExpression.And and -> {
                collectDimensions(and.left(), dimensions);
                collectDimensions(and.right(), dimensions);
            }
            case LogicalExpression.Or or -> {
                collectDimensions(or.left(), dimensions);
                collectDimensions(or.right(), dimensions);
            }
            case LogicalExpression.Not not -> collectDimensions(not.expression(), dimensions);
            case LogicalExpression.ComparisonWrap wrap -> collectDimensions(wrap.comparison(), dimensions);
            case LogicalExpression.AlwaysTrue ignored -> {
            }
            case LogicalExpression.AlwaysFalse ignored -> {
            }
        }
    }

    private static void collectDimensions(final ComparisonExpression comparison, final Set<Dimension> dimensions) {
        switch (comparison) {
            case ComparisonExpression.Equal eq -> {
                collectDimension(eq.left(), dimensions);
                collectDimension(eq.right(), dimensions);
            }
            case ComparisonExpression.NotEqual ne -> {
                collectDimension(ne.left(), dimensions);
                collectDimension(ne.right(), dimensions);
            }
            case ComparisonExpression.In in -> {
                dimensions.add(in.left().dimension());
                in.rights().forEach(fixed -> dimensions.add(fixed.partition().getDimension()));
            }
        }
    }

    private static void collectDimension(final PartitionExpression expr, final Set<Dimension> dimensions) {
        switch (expr) {
            case PartitionExpression.Relative rel -> dimensions.add(rel.dimension());
            case PartitionExpression.Fixed fixed -> dimensions.add(fixed.partition().getDimension());
            case PartitionExpression.Null ignored -> {
            }
        }
    }
}
