package io.github.march_plugin.core.config.rules.model.ast;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LogicalExpressionToStringTest {

    private static LogicalExpression.ComparisonWrap eqService;
    private static LogicalExpression.ComparisonWrap eqUI;

    @BeforeAll
    static void setUp() {
        final var layerDimBuilder = new Dimension.Builder("layer");
        layerDimBuilder.addPartition("service");
        layerDimBuilder.addPartition("ui");
        final var layerDim = layerDimBuilder.build();

        final var sourceLayer = new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim);
        final var fixedService = new PartitionExpression.Fixed(layerDim.getPartition("service"));
        final var fixedUI = new PartitionExpression.Fixed(layerDim.getPartition("ui"));

        eqService = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(sourceLayer, fixedService));
        eqUI = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(sourceLayer, fixedUI));
    }

    @ParameterizedTest(name = "expression: {0}, expected: {1}")
    @MethodSource("provideFormattingCases")
    void shouldFormatToString(final LogicalExpression expression, final String expectedString) {
        assertThat(expression.toString()).isEqualTo(expectedString);
    }

    static Stream<Arguments> provideFormattingCases() {
        return Stream.of(
                Arguments.of(eqService, "source.layer == layer.service"),
                Arguments.of(new LogicalExpression.Not(eqService), "!source.layer == layer.service"),
                Arguments.of(new LogicalExpression.Group(eqService), "(source.layer == layer.service)"),
                Arguments.of(new LogicalExpression.And(eqService, eqUI), "source.layer == layer.service AND source.layer == layer.ui"),
                Arguments.of(new LogicalExpression.Or(eqService, eqUI), "source.layer == layer.service OR source.layer == layer.ui")
        );
    }
}