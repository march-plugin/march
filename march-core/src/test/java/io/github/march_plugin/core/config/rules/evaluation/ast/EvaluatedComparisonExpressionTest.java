package io.github.march_plugin.core.config.rules.evaluation.ast;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluatedComparisonExpressionTest {

    private static EvaluatedPartitionExpression.Fixed article;
    private static EvaluatedPartitionExpression.Fixed order;
    private static EvaluatedPartitionExpression.Fixed user;
    private static EvaluatedPartitionExpression.Relative sourceDomain;

    @BeforeAll
    static void setUp() {
        final var domainBuilder = new Dimension.Builder("domain");
        final var articlePartition = domainBuilder.addPartition("article");
        final var orderPartition = domainBuilder.addPartition("order");
        final var userPartition = domainBuilder.addPartition("user");
        final var domain = domainBuilder.build();

        article = new EvaluatedPartitionExpression.Fixed(articlePartition);
        order = new EvaluatedPartitionExpression.Fixed(orderPartition);
        user = new EvaluatedPartitionExpression.Fixed(userPartition);
        sourceDomain = new EvaluatedPartitionExpression.Relative(EvaluatedPartitionExpression.Relative.Side.SOURCE, domain);
    }

    @Test
    void shouldTreatInAsEqualRegardlessOfRightsOrder() {
        final var first = new EvaluatedComparisonExpression.In(sourceDomain, List.of(article, order, user));
        final var second = new EvaluatedComparisonExpression.In(sourceDomain, List.of(user, article, order));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldMatchHashCodeWheneverInIsEqual() {
        final var first = new EvaluatedComparisonExpression.In(sourceDomain, List.of(article, order, user));
        final var second = new EvaluatedComparisonExpression.In(sourceDomain, List.of(user, article, order));

        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldDeduplicateReorderedInInAHashSet() {
        final var first = new EvaluatedComparisonExpression.In(sourceDomain, List.of(article, order, user));
        final var second = new EvaluatedComparisonExpression.In(sourceDomain, List.of(user, article, order));

        final var seen = new HashSet<EvaluatedComparisonExpression>();
        seen.add(first);
        seen.add(second);

        assertThat(seen).hasSize(1);
    }

    @Test
    void shouldNotBeEqualWhenRightsDiffer() {
        final var first = new EvaluatedComparisonExpression.In(sourceDomain, List.of(article, order));
        final var second = new EvaluatedComparisonExpression.In(sourceDomain, List.of(article, user));

        assertThat(first).isNotEqualTo(second);
    }
}
