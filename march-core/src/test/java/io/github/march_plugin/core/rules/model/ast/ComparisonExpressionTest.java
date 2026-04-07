package io.github.march_plugin.core.rules.model.ast;

import io.github.march_plugin.core.dimensions.model.Dimension;
import io.github.march_plugin.core.rules.exceptions.ConstantComparisonException;
import io.github.march_plugin.core.rules.exceptions.DimensionMismatchException;
import io.github.march_plugin.core.rules.exceptions.DuplicatePartitionException;
import io.github.march_plugin.core.rules.exceptions.NullComparisonException;
import io.github.march_plugin.core.rules.exceptions.RedundantComparisonException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparisonExpressionTest {

    private static Dimension layerDim;
    private static Dimension.Partition serviceLayer;
    private static Dimension.Partition uiLayer;
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
                Arguments.of(sourceLayer, sourceLayer, RedundantComparisonException.class),
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
}