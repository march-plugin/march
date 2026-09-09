package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleReducerTest {

    private static final RuleReducer REDUCER = new RuleReducer();

    private static Dimension layerDim;
    private static Dimension regionDim;

    private static Dimension.Partition servicePart;
    private static Dimension.Partition uiPart;

    @BeforeAll
    static void setUp() {
        final var layerBuilder = new Dimension.Builder("layer");
        servicePart = layerBuilder.addPartition("service");
        uiPart = layerBuilder.addPartition("ui");
        layerDim = layerBuilder.build();

        final var regionBuilder = new Dimension.Builder("region");
        regionBuilder.addPartition("eu");
        regionBuilder.addPartition("us");
        regionDim = regionBuilder.build();
    }

    private static LogicalExpression.ComparisonWrap equalOnBothSides(final Dimension dimension) {
        return new LogicalExpression.ComparisonWrap(
                new ComparisonExpression.Equal(
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, dimension),
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, dimension)
                )
        );
    }

    private static LogicalExpression.ComparisonWrap notEqualOnBothSides(final Dimension dimension) {
        return new LogicalExpression.ComparisonWrap(
                new ComparisonExpression.NotEqual(
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, dimension),
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, dimension)
                )
        );
    }

    private LogicalExpression reduce(final LogicalExpression expr, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target) {
        return REDUCER.reduce(expr, source, target, Map.of(), Map.of());
    }

    @Test
    void resolvesEqualToTrueWhenBothSidesAreClassifiedAndMatch() {
        final var result = reduce(equalOnBothSides(layerDim), Set.of(servicePart), Set.of(servicePart));

        assertThat(result).isEqualTo(new LogicalExpression.AlwaysTrue());
    }

    @Test
    void resolvesEqualToFalseWhenBothSidesAreClassifiedButDiffer() {
        final var result = reduce(equalOnBothSides(layerDim), Set.of(servicePart), Set.of(uiPart));

        assertThat(result).isEqualTo(new LogicalExpression.AlwaysFalse());
    }

    @Test
    void doesNotResolveEqualToTrueWhenNeitherSideClassifiesTheDimension() {
        // Neither source nor target has any partition for "region" at all: the reducer cannot know whether
        // they would actually match once that dimension is eventually filled in, so it must stay open rather
        // than silently collapsing null == null to true.
        final var result = reduce(equalOnBothSides(regionDim), Set.of(servicePart), Set.of(uiPart));

        assertThat(result).isNotEqualTo(new LogicalExpression.AlwaysTrue());
        assertThat(result).isInstanceOf(LogicalExpression.ComparisonWrap.class);
    }

    @Test
    void doesNotResolveNotEqualToTrueWhenOnlyOneSideClassifiesTheDimension() {
        // Only source classifies "layer": whether it is genuinely "different" from target's (unknown) layer
        // cannot be decided yet, so the comparison must stay open rather than resolving to true.
        final var result = reduce(notEqualOnBothSides(layerDim), Set.of(servicePart), Set.of());

        assertThat(result).isNotEqualTo(new LogicalExpression.AlwaysTrue());
        assertThat(result).isInstanceOf(LogicalExpression.ComparisonWrap.class);
    }

    @Test
    void resolvesNotEqualToTrueWhenBothSidesAreClassifiedAndDiffer() {
        final var result = reduce(notEqualOnBothSides(layerDim), Set.of(servicePart), Set.of(uiPart));

        assertThat(result).isEqualTo(new LogicalExpression.AlwaysTrue());
    }

    @Test
    void resolvesNotEqualToFalseWhenBothSidesAreClassifiedAndMatch() {
        final var result = reduce(notEqualOnBothSides(layerDim), Set.of(servicePart), Set.of(servicePart));

        assertThat(result).isEqualTo(new LogicalExpression.AlwaysFalse());
    }

    @Test
    void explicitNullLiteralComparisonStillResolvesAgainstAnUnclassifiedRelative() {
        // "source.layer != NULL" is the dedicated absence check, distinct from comparing two relatives to
        // each other, and must keep resolving definitively once the relative side is itself resolved.
        final var isNotNull = new LogicalExpression.ComparisonWrap(
                new ComparisonExpression.NotEqual(
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim),
                        new PartitionExpression.Null()
                )
        );

        assertThat(reduce(isNotNull, Set.of(servicePart), Set.of())).isEqualTo(new LogicalExpression.AlwaysTrue());
    }

    @Test
    void forcedValuesAreTreatedAsResolvedForEquality() {
        final var forcedSource = Map.<Dimension, Dimension.Partition>of(layerDim, servicePart);

        final var result = REDUCER.reduce(equalOnBothSides(layerDim), Set.of(), Set.of(servicePart), forcedSource, Map.of());

        assertThat(result).isEqualTo(new LogicalExpression.AlwaysTrue());
    }
}
