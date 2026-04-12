package io.github.march_plugin.core.config.rules.model.ast;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.exceptions.DimensionMismatchException;
import io.github.march_plugin.core.config.rules.exceptions.NullComparisonException;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparisonExpressionToStringTest {

    private static Dimension layerDim;
    private static Dimension.Partition serviceLayer;
    private static Dimension.Partition uiLayer;
    private static Dimension regionDim;
    private static Dimension.Partition euRegion;
    private static Dimension.Partition usRegion;

    private static PartitionExpression.Relative sourceLayer;
    private static PartitionExpression.Relative targetLayer;
    private static PartitionExpression.Relative sourceRegion;

    private static PartitionExpression.Fixed fixedService;
    private static PartitionExpression.Fixed fixedUI;
    private static PartitionExpression.Fixed fixedEU;

    private static PartitionExpression.Null nullExpr;

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
        targetLayer = new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, layerDim);
        sourceRegion = new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, regionDim);

        fixedService = new PartitionExpression.Fixed(layerDim.getPartition("service"));
        fixedUI = new PartitionExpression.Fixed(layerDim.getPartition("ui"));
        fixedEU = new PartitionExpression.Fixed(regionDim.getPartition("eu"));

        nullExpr = new PartitionExpression.Null();
    }

    @ParameterizedTest(name = "expression: {0}, expected: {1}")
    @MethodSource("provideFormattingCases")
    void shouldFormatToString(final ComparisonExpression expression, final String expectedString) {
        assertThat(expression.toString()).isEqualTo(expectedString);
    }

    static Stream<Arguments> provideFormattingCases() {
        return Stream.of(
                Arguments.of(new ComparisonExpression.Equal(sourceLayer, fixedService), "source.layer == layer.service"),
                Arguments.of(new ComparisonExpression.Equal(targetLayer, fixedUI), "target.layer == layer.ui"),

                Arguments.of(new ComparisonExpression.Equal(sourceLayer, targetLayer), "source.layer == target.layer"),
                Arguments.of(new ComparisonExpression.NotEqual(sourceRegion, new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, regionDim)), "source.region != target.region"),

                Arguments.of(new ComparisonExpression.NotEqual(sourceLayer, nullExpr), "source.layer != NULL"),
                Arguments.of(new ComparisonExpression.Equal(targetLayer, nullExpr), "target.layer == NULL"),

                Arguments.of(new ComparisonExpression.In(sourceLayer, List.of(fixedService)), "source.layer IN layer.(service)"),
                Arguments.of(new ComparisonExpression.In(sourceLayer, List.of(fixedService, fixedUI)), "source.layer IN layer.(service|ui)"),
                Arguments.of(new ComparisonExpression.In(sourceRegion, List.of(fixedEU)), "source.region IN region.(eu)")
        );
    }

    @ParameterizedTest(name = "In expression with {0} and {1} should throw NullComparisonException")
    @MethodSource("provideNullOrEmptyInArguments")
    void shouldThrowOnInWithNullOrEmptyRights(final PartitionExpression.Relative left, final List<PartitionExpression.Fixed> rights) {
        assertThatThrownBy(() -> new ComparisonExpression.In(left, rights))
                .isInstanceOf(NullComparisonException.class);
    }

    static Stream<Arguments> provideNullOrEmptyInArguments() {
        return Stream.of(
                Arguments.of(sourceLayer, null),
                Arguments.of(sourceLayer, List.of()),
                Arguments.of(null, List.of(fixedService))
        );
    }

    @Test
    void shouldThrowOnInWithMixedDimensions() {
        assertThatThrownBy(() -> new ComparisonExpression.In(sourceLayer, List.of(fixedService, fixedEU)))
                .isInstanceOf(DimensionMismatchException.class);
    }
}