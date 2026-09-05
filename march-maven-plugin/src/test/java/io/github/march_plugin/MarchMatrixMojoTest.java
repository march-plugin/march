package io.github.march_plugin;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarchMatrixMojoTest {

    private static Dimension dimension(final String name) {
        final var builder = new Dimension.Builder(name);
        builder.addPartition("p1");
        builder.addPartition("p2");
        return builder.build();
    }

    @Nested
    class ColumnWidthValidation {

        @Test
        void shouldThrowWhenColumnWidthIsZero() {
            assertThatThrownBy(() -> MarchMatrixMojo.validateColumnWidth(0))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("march.columnWidth");
        }

        @Test
        void shouldThrowWhenColumnWidthIsNegative() {
            final var negativeColumnWidth = -3;

            assertThatThrownBy(() -> MarchMatrixMojo.validateColumnWidth(negativeColumnWidth))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("march.columnWidth");
        }

        @Test
        void shouldAcceptMinimumValidColumnWidthOfOne() {
            assertThatCode(() -> MarchMatrixMojo.validateColumnWidth(1)).doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptWideColumnWidth() {
            final var wideColumnWidth = 50;

            assertThatCode(() -> MarchMatrixMojo.validateColumnWidth(wideColumnWidth)).doesNotThrowAnyException();
        }
    }

    @Nested
    class NoCombinationMessage {

        @Test
        void shouldNameEveryRequestedDimensionSortedAlphabetically() {
            final var domainBuilder = new Dimension.Builder("domain");
            domainBuilder.addPartition("article");
            domainBuilder.addPartition("order");
            final var domain = domainBuilder.build();

            final var artifactBuilder = new Dimension.Builder("artifact");
            artifactBuilder.addPartition("component");
            artifactBuilder.addPartition("deployment");
            final var artifact = artifactBuilder.build();

            final var message = MarchMatrixMojo.noCombinationMessage(Set.of(domain, artifact));

            assertThat(message)
                    .contains("artifact")
                    .contains("domain")
                    .contains("-Dclassifications")
                    .containsSubsequence("artifact", "domain");
        }
    }

    @Nested
    class Truncate {

        @Test
        void shouldReturnUnchangedTextWhenShorterThanLimit() {
            assertThat(MarchMatrixMojo.truncate("ab", 5)).isEqualTo("ab");
        }

        @Test
        void shouldReturnUnchangedTextWhenExactlyAtLimit() {
            assertThat(MarchMatrixMojo.truncate("abcde", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldTruncateTextLongerThanLimit() {
            assertThat(MarchMatrixMojo.truncate("abcdefgh", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldReturnEmptyStringForNullInput() {
            assertThat(MarchMatrixMojo.truncate(null, 5)).isEmpty();
        }

        @Test
        void shouldDistinguishLabelsThatCollideAtNarrowerWidth() {
            final var wideEnoughToDistinguish = 8;

            assertThat(MarchMatrixMojo.truncate("adapterIn", 5)).isEqualTo(MarchMatrixMojo.truncate("adapterOut", 5));
            assertThat(MarchMatrixMojo.truncate("adapterIn", wideEnoughToDistinguish))
                    .isNotEqualTo(MarchMatrixMojo.truncate("adapterOut", wideEnoughToDistinguish));
        }
    }

    @Nested
    class Center {

        @Test
        void shouldPadShortTextEvenlyOnBothSides() {
            final var evenWidth = 6;

            assertThat(MarchMatrixMojo.center("ok", evenWidth)).isEqualTo("  ok  ");
        }

        @Test
        void shouldFavorRightPaddingWhenPaddingIsOdd() {
            assertThat(MarchMatrixMojo.center("ok", 5)).isEqualTo(" ok  ");
        }

        @Test
        void shouldReturnTextUnchangedWhenExactlyAtWidth() {
            assertThat(MarchMatrixMojo.center("abcde", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldTruncateTextLongerThanWidth() {
            assertThat(MarchMatrixMojo.center("abcdefgh", 5)).isEqualTo("abcde");
        }

        @Test
        void shouldPadEmptyTextToFullWidth() {
            assertThat(MarchMatrixMojo.center("", 4)).isEqualTo("    ");
        }
    }

    @Nested
    class PartitionOrdering {

        @Test
        void shouldOrderPartitionsToMatchTheGivenDimensionOrder() {
            final var domain = dimension("domain");
            final var layer = dimension("layer");
            final var abstraction = dimension("abstraction");
            final var combination = Set.of(layer.getPartition("p1"), abstraction.getPartition("p1"), domain.getPartition("p1"));

            final var ordered = MarchMatrixMojo.orderPartitions(combination, List.of(domain, layer, abstraction));

            assertThat(ordered).containsExactly(domain.getPartition("p1"), layer.getPartition("p1"), abstraction.getPartition("p1"));
        }

        @Test
        void shouldProduceIdenticalOrderRegardlessOfSetIterationOrder() {
            final var domain = dimension("domain");
            final var layer = dimension("layer");
            final var abstraction = dimension("abstraction");
            final var dimensionOrder = List.of(domain, layer, abstraction);

            final var forwardInsertion = new LinkedHashSet<Dimension.Partition>();
            forwardInsertion.add(domain.getPartition("p1"));
            forwardInsertion.add(layer.getPartition("p2"));
            forwardInsertion.add(abstraction.getPartition("p1"));

            final var reverseInsertion = new LinkedHashSet<Dimension.Partition>();
            reverseInsertion.add(abstraction.getPartition("p1"));
            reverseInsertion.add(layer.getPartition("p2"));
            reverseInsertion.add(domain.getPartition("p1"));

            assertThat(MarchMatrixMojo.orderPartitions(forwardInsertion, dimensionOrder))
                    .isEqualTo(MarchMatrixMojo.orderPartitions(reverseInsertion, dimensionOrder));
        }
    }

    @Nested
    class CombinationComparison {

        @Test
        void shouldOrderByDeclaredPositionNotAlphabetically() {
            final var domainBuilder = new Dimension.Builder("domain");
            final var order = domainBuilder.addPartition("order");
            final var article = domainBuilder.addPartition("article");
            domainBuilder.build();

            // "order" is declared before "article" here, so declared-order sorting must rank it first
            // even though "article" < "order" alphabetically.
            assertThat(MarchMatrixMojo.compareCombinations(List.of(order), List.of(article))).isNegative();
            assertThat(MarchMatrixMojo.compareCombinations(List.of(article), List.of(order))).isPositive();
        }

        @Test
        void shouldFallThroughToLaterPositionsWhenEarlierOnesAreEqual() {
            final var domainBuilder = new Dimension.Builder("domain");
            final var article = domainBuilder.addPartition("article");
            domainBuilder.addPartition("order");
            domainBuilder.build();

            final var layerBuilder = new Dimension.Builder("layer");
            final var presentation = layerBuilder.addPartition("presentation");
            final var service = layerBuilder.addPartition("service");
            layerBuilder.addPartition("business");
            layerBuilder.build();

            assertThat(MarchMatrixMojo.compareCombinations(List.of(article, presentation), List.of(article, service))).isNegative();
        }

        @Test
        void shouldReturnZeroForIdenticalCombinations() {
            final var domain = dimension("domain");
            final var combo = List.of(domain.getPartition("p1"), domain.getPartition("p1"));

            assertThat(MarchMatrixMojo.compareCombinations(combo, combo)).isZero();
        }

        @Test
        void sortingByThisComparatorShouldFollowDeclaredConfigFileOrderNotAlphabeticalOrder() {
            final var domainBuilder = new Dimension.Builder("domain");
            final var order = domainBuilder.addPartition("order");
            final var article = domainBuilder.addPartition("article");
            domainBuilder.build();

            final var layerBuilder = new Dimension.Builder("layer");
            final var presentation = layerBuilder.addPartition("presentation");
            final var service = layerBuilder.addPartition("service");
            layerBuilder.build();

            final var combos = new ArrayList<>(List.of(
                    List.of(article, service),
                    List.of(order, presentation),
                    List.of(order, service),
                    List.of(article, presentation)));

            combos.sort(MarchMatrixMojo::compareCombinations);

            // "order" was declared before "article" and "presentation" before "service" — the sort must
            // follow that declared order, not alphabetical order (which would put "article" first).
            assertThat(combos).containsExactly(
                    List.of(order, presentation),
                    List.of(order, service),
                    List.of(article, presentation),
                    List.of(article, service));
        }
    }
}
