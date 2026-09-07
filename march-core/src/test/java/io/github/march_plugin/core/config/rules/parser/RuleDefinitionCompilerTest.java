package io.github.march_plugin.core.config.rules.parser;

import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.exceptions.RedundantComparisonException;
import io.github.march_plugin.core.config.rules.exceptions.RedundantLogicalOperationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleDefinitionCompilerTest {

    private static DimensionRegistry dimensionRegistry;

    @BeforeAll
    static void setUp() {
        final var layerBuilder = new Dimension.Builder("layer");
        layerBuilder.addPartition("service");
        layerBuilder.addPartition("web");
        final var layer = layerBuilder.build();

        dimensionRegistry = new DimensionRegistry.Builder().addDimension(layer).build();
    }

    @Test
    void shouldCompileAValidRule() {
        final var expression = new RuleDefinitionCompiler(dimensionRegistry).compile("source.layer == layer.service");

        assertThat(expression).isNotNull();
    }

    @Test
    void shouldRejectRedundantComparison() {
        assertThatThrownBy(() -> new RuleDefinitionCompiler(dimensionRegistry).compile("source.layer == source.layer"))
                .isInstanceOf(RedundantComparisonException.class);
    }

    @Test
    void shouldRejectRedundantAnd() {
        assertThatThrownBy(() -> new RuleDefinitionCompiler(dimensionRegistry).compile(
                "source.layer == layer.service AND source.layer == layer.service"))
                .isInstanceOf(RedundantLogicalOperationException.class);
    }

    @Test
    void shouldRejectRedundantOr() {
        assertThatThrownBy(() -> new RuleDefinitionCompiler(dimensionRegistry).compile(
                "source.layer == layer.service OR source.layer == layer.service"))
                .isInstanceOf(RedundantLogicalOperationException.class);
    }

    @Test
    void shouldRejectRedundancyNestedInsideAValidRule() {
        assertThatThrownBy(() -> new RuleDefinitionCompiler(dimensionRegistry).compile(
                "target.layer == layer.web AND (source.layer == layer.service OR source.layer == layer.service)"))
                .isInstanceOf(RedundantLogicalOperationException.class);
    }

    @Test
    void shouldRejectReorderedInAsRedundantAnd() {
        assertThatThrownBy(() -> new RuleDefinitionCompiler(dimensionRegistry).compile(
                "source.layer IN layer.(service|web) AND source.layer IN layer.(web|service)"))
                .isInstanceOf(RedundantLogicalOperationException.class);
    }

    @Test
    void shouldAllowInWithDifferentOptionsNextToEachOther() {
        final var expression = new RuleDefinitionCompiler(dimensionRegistry).compile(
                "source.layer IN layer.(service|web) AND target.layer == layer.web");

        assertThat(expression).isNotNull();
    }
}
