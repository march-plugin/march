package io.github.march_plugin;

import io.github.march_plugin.core.config.dimensions.exceptions.DimensionNotFoundException;
import io.github.march_plugin.core.config.dimensions.exceptions.PartitionNotFoundException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatrixClassificationParserTest {

    private final MatrixClassificationParser parser = new MatrixClassificationParser();

    private Dimension domain;
    private Dimension.Partition article;
    private Dimension.Partition order;

    private Dimension layer;
    private Dimension.Partition api;
    private Dimension.Partition impl;

    private DimensionRegistry dimensionRegistry;

    @BeforeEach
    void setUp() {
        final var domainBuilder = new Dimension.Builder("domain");
        article = domainBuilder.addPartition("article");
        order = domainBuilder.addPartition("order");
        domain = domainBuilder.build();

        final var layerBuilder = new Dimension.Builder("layer");
        api = layerBuilder.addPartition("api");
        impl = layerBuilder.addPartition("impl");
        layer = layerBuilder.build();

        dimensionRegistry = new DimensionRegistry.Builder()
                .addDimension(domain)
                .addDimension(layer)
                .build();
    }

    @Nested
    class DefaultingWhenInputMissing {

        @Test
        void shouldUseAllDimensionsWithAllPartitionsWhenInputIsNull() {
            final var result = parser.parse(null, dimensionRegistry);

            assertThat(result).containsExactlyInAnyOrder(
                    Set.of(article, order),
                    Set.of(api, impl));
        }

        @Test
        void shouldUseAllDimensionsWithAllPartitionsWhenInputIsEmpty() {
            final var result = parser.parse("", dimensionRegistry);

            assertThat(result).containsExactlyInAnyOrder(
                    Set.of(article, order),
                    Set.of(api, impl));
        }
    }

    @Nested
    class ExplicitInput {

        @Test
        void shouldResolveAllPartitionsWhenNoneAreListed() {
            final var result = parser.parse("{domain}", dimensionRegistry);

            assertThat(result).containsExactly(Set.of(article, order));
        }

        @Test
        void shouldResolveExplicitlyListedPartition() {
            final var result = parser.parse("{domain(article)}", dimensionRegistry);

            assertThat(result).containsExactly(Set.of(article));
        }

        @Test
        void shouldResolveMultipleExplicitlyListedPartitions() {
            final var result = parser.parse("{layer(api;impl)}", dimensionRegistry);

            assertThat(result).containsExactly(Set.of(api, impl));
        }

        @Test
        void shouldResolveMultipleDimensionsSeparatedBySemicolon() {
            final var result = parser.parse("{domain(article);layer(api)}", dimensionRegistry);

            assertThat(result).containsExactlyInAnyOrder(Set.of(article), Set.of(api));
        }

        @Test
        void shouldTolerateInputWithoutSurroundingBraces() {
            final var result = parser.parse("domain(article)", dimensionRegistry);

            assertThat(result).containsExactly(Set.of(article));
        }

        @Test
        void shouldTrimWhitespaceAroundPartitionNamesWithinParentheses() {
            final var result = parser.parse("{domain(article ; order)}", dimensionRegistry);

            assertThat(result).containsExactly(Set.of(article, order));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldThrowWhenDimensionNameIsUnknown() {
            assertThatThrownBy(() -> parser.parse("{unknown}", dimensionRegistry))
                    .isInstanceOf(DimensionNotFoundException.class);
        }

        @Test
        void shouldThrowWhenPartitionNameIsUnknown() {
            assertThatThrownBy(() -> parser.parse("{domain(unknown)}", dimensionRegistry))
                    .isInstanceOf(PartitionNotFoundException.class);
        }
    }
}
