package io.github.march_plugin.core.config.rules.model.ast;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.exceptions.ConstantComparisonException;
import io.github.march_plugin.core.config.rules.exceptions.DimensionMismatchException;
import io.github.march_plugin.core.config.rules.exceptions.DuplicatePartitionException;
import io.github.march_plugin.core.config.rules.exceptions.NullComparisonException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparisonExpressionTest {

    private static Dimension layerDim;
    private static Dimension.Partition serviceLayer;
    private static Dimension.Partition uiLayer;
    private static Dimension.Partition webLayer;
    private static Dimension regionDim;
    private static Dimension.Partition euRegion;
    private static Dimension.Partition usRegion;
    private static PartitionExpression.Relative sourceLayer;
    private static PartitionExpression.Fixed fixedService;

    @BeforeAll
    static void setUp() {
        final var layerDimBuilder = new Dimension.Builder("layer");
        serviceLayer = layerDimBuilder.addPartition("service");
        uiLayer = layerDimBuilder.addPartition("ui");
        webLayer = layerDimBuilder.addPartition("web");
        layerDim = layerDimBuilder.build();

        final var layerDimBuilder2 = new Dimension.Builder("region");
        euRegion = layerDimBuilder2.addPartition("eu");
        usRegion = layerDimBuilder2.addPartition("us");
        regionDim = layerDimBuilder2.build();

        sourceLayer = new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim);
        fixedService = new PartitionExpression.Fixed(serviceLayer);
    }

    @ParameterizedTest(name = "Comparison of {0} and {1} should throw {2}")
    @MethodSource("provideInvalidCombinations")
    void shouldThrowTypedExceptions(final PartitionExpression left, final PartitionExpression right, final Class<? extends Throwable> expected) {
        assertThatThrownBy(() -> new ComparisonExpression.Equal(left, right)).isInstanceOf(expected);
        assertThatThrownBy(() -> new ComparisonExpression.NotEqual(left, right)).isInstanceOf(expected);
    }

    static Stream<Arguments> provideInvalidCombinations() {
        final var targetRegion = new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, regionDim);
        final var fixedUI = new PartitionExpression.Fixed(uiLayer);

        return Stream.of(
                // Existing cases
                Arguments.of(fixedService, fixedUI, ConstantComparisonException.class),
                Arguments.of(sourceLayer, targetRegion, DimensionMismatchException.class),
                Arguments.of(null, sourceLayer, NullComparisonException.class),
                Arguments.of(sourceLayer, null, NullComparisonException.class),
                Arguments.of(sourceLayer, new PartitionExpression.Fixed(euRegion), DimensionMismatchException.class)
        );
    }

    @Test
    void shouldThrowOnInDimensionMismatch() {
        final var fixedEU = new PartitionExpression.Fixed(euRegion);
        assertThatThrownBy(() -> new ComparisonExpression.In(sourceLayer, List.of(fixedEU)))
                .isInstanceOf(DimensionMismatchException.class);
    }

    @Test
    void shouldThrowOnInDuplicates() {
        assertThatThrownBy(() -> new ComparisonExpression.In(sourceLayer, List.of(fixedService, fixedService)))
                .isInstanceOf(DuplicatePartitionException.class);
    }

    @Nested
    class InEqualityIgnoresOrder {

        @Test
        void shouldTreatReorderedRightsAsEqual() {
            final var first = new ComparisonExpression.In(sourceLayer, List.of(fixedService, new PartitionExpression.Fixed(uiLayer), new PartitionExpression.Fixed(webLayer)));
            final var second = new ComparisonExpression.In(sourceLayer, List.of(new PartitionExpression.Fixed(webLayer), fixedService, new PartitionExpression.Fixed(uiLayer)));

            assertThat(first).isEqualTo(second);
        }

        @Test
        void shouldMatchHashCodeWheneverEqual() {
            final var first = new ComparisonExpression.In(sourceLayer, List.of(fixedService, new PartitionExpression.Fixed(uiLayer)));
            final var second = new ComparisonExpression.In(sourceLayer, List.of(new PartitionExpression.Fixed(uiLayer), fixedService));

            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        void shouldDeduplicateReorderedInInAHashSet() {
            final var first = new ComparisonExpression.In(sourceLayer, List.of(fixedService, new PartitionExpression.Fixed(uiLayer)));
            final var second = new ComparisonExpression.In(sourceLayer, List.of(new PartitionExpression.Fixed(uiLayer), fixedService));

            final var seen = new HashSet<ComparisonExpression>();
            seen.add(first);
            seen.add(second);

            assertThat(seen).hasSize(1);
        }

        @Test
        void shouldNotBeEqualWhenRightsDiffer() {
            final var first = new ComparisonExpression.In(sourceLayer, List.of(fixedService, new PartitionExpression.Fixed(uiLayer)));
            final var second = new ComparisonExpression.In(sourceLayer, List.of(fixedService, new PartitionExpression.Fixed(webLayer)));

            assertThat(first).isNotEqualTo(second);
        }
    }
}
