package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.classification.model.Classification;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.evaluation.RuleEvaluator;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleEvaluatorTest {

    private static final RuleEvaluator EVALUATOR = new RuleEvaluator();

    private static Dimension layerDim;
    private static Dimension regionDim;

    private static Dimension.Partition servicePart;
    private static Dimension.Partition uiPart;
    private static Dimension.Partition euPart;
    private static Dimension.Partition usPart;

    private static LogicalExpression.ComparisonWrap sourceIsService;
    private static LogicalExpression.ComparisonWrap targetIsUI;
    private static LogicalExpression.ComparisonWrap sourceIsEU;

    private static Classification serviceClassification;
    private static Classification uiClassification;
    private static Classification emptyClassification;
    private static Classification euClassification;

    @BeforeAll
    static void setUp() {
        final var layerBuilder = new Dimension.Builder("layer");
        servicePart = layerBuilder.addPartition("service");
        uiPart = layerBuilder.addPartition("ui");
        layerDim = layerBuilder.build();

        final var regionBuilder = new Dimension.Builder("region");
        euPart = regionBuilder.addPartition("eu");
        usPart = regionBuilder.addPartition("us");
        regionDim = regionBuilder.build();

        sourceIsService = new LogicalExpression.ComparisonWrap(
                new ComparisonExpression.Equal(
                        new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim),
                        new PartitionExpression.Fixed(servicePart)
                ));

        targetIsUI = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, layerDim),
                new PartitionExpression.Fixed(uiPart)
        ));

        sourceIsEU = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(
                new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, regionDim),
                new PartitionExpression.Fixed(euPart)
        ));

        serviceClassification = new Classification.Builder().addPartition(servicePart).build();
        uiClassification = new Classification.Builder().addPartition(uiPart).build();
        emptyClassification = new Classification.Builder().build();
        euClassification = new Classification.Builder().addPartition(euPart).build();
    }

    @Nested
    class ComparisonEvaluation {

        @Test
        void shouldEvaluateEquality() {
            assertThat(EVALUATOR.evaluate(sourceIsService, serviceClassification, uiClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(sourceIsService, uiClassification, uiClassification)).isFalse();
        }

        @Test
        void shouldEvaluateInequality() {
            final var ne = new LogicalExpression.ComparisonWrap(
                    new ComparisonExpression.NotEqual(
                            new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim),
                            new PartitionExpression.Fixed(servicePart)
                    )
            );
            assertThat(EVALUATOR.evaluate(ne, uiClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(ne, serviceClassification, emptyClassification)).isFalse();
        }

        @Test
        void shouldEvaluateInExpression() {
            final var inExpr = new LogicalExpression.ComparisonWrap(
                    new ComparisonExpression.In(
                            new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim),
                            List.of(new PartitionExpression.Fixed(servicePart), new PartitionExpression.Fixed(uiPart))
                    )
            );
            assertThat(EVALUATOR.evaluate(inExpr, serviceClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(inExpr, uiClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(inExpr, euClassification, emptyClassification)).isFalse();
        }

        @Test
        void shouldHandleNullResolutionsGracefully() {
            assertThat(EVALUATOR.evaluate(sourceIsService, euClassification, emptyClassification)).isFalse();
        }

        @Test
        void shouldEvaluateAgainstNullLiteral() {
            final var isNull = new LogicalExpression.ComparisonWrap(
                    new ComparisonExpression.Equal(
                            new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim),
                            new PartitionExpression.Null()
                    )
            );
            assertThat(EVALUATOR.evaluate(isNull, euClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(isNull, serviceClassification, emptyClassification)).isFalse();
        }
    }

    @Nested
    class LogicalComposition {

        @Test
        void shouldEvaluateAnd() {
            final var and = new LogicalExpression.And(sourceIsService, targetIsUI);
            assertThat(EVALUATOR.evaluate(and, serviceClassification, uiClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(and, uiClassification, uiClassification)).isFalse();
        }

        @Test
        void shouldEvaluateOr() {
            final var or = new LogicalExpression.Or(sourceIsService, sourceIsEU);
            assertThat(EVALUATOR.evaluate(or, serviceClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(or, euClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(or, uiClassification, emptyClassification)).isFalse();
        }

        @Test
        void shouldEvaluateNot() {
            final var not = new LogicalExpression.Not(sourceIsService);
            assertThat(EVALUATOR.evaluate(not, uiClassification, emptyClassification)).isTrue();
            assertThat(EVALUATOR.evaluate(not, serviceClassification, emptyClassification)).isFalse();
        }

        @Test
        void shouldHandleDeeplyNestedLogic() {
            final var complex = new LogicalExpression.Not(
                    new LogicalExpression.And(sourceIsService, targetIsUI)
            );
            assertThat(EVALUATOR.evaluate(complex, serviceClassification, uiClassification)).isFalse();
            assertThat(EVALUATOR.evaluate(complex, serviceClassification, emptyClassification)).isTrue();
        }
    }

    @Nested
    class ClassificationIntegrity {

        @Test
        void shouldAllowDuplicateDimensionWhenBuiltDirectly() {
            final var classification = new Classification.Builder()
                    .addPartition(servicePart)
                    .addPartition(uiPart)
                    .build();

            assertThat(classification.getPartitions()).containsExactlyInAnyOrder(servicePart, uiPart);
        }

        @Test
        void shouldMaintainImmutabilityOfPartitions() {
            final var partitions = serviceClassification.getPartitions();
            assertThatThrownBy(partitions::clear)
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void buildChildClassificationShouldMergePartitionsWithoutDuplicateCheck() {
            final var merged = Classification.Builder.buildChildClassification(serviceClassification, uiPart);

            assertThat(merged.getPartitions()).containsExactlyInAnyOrder(servicePart, uiPart);
        }
    }

    @Test
    void shouldHandleMissingDimensionInClassification() {
        final var result = EVALUATOR.evaluate(
                sourceIsService,
                euClassification,
                emptyClassification
        );

        assertThat(result).isFalse();
    }
}