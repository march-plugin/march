package io.github.march_plugin.core.rules.model.ast;

import io.github.march_plugin.core.dimensions.model.Dimension;
import io.github.march_plugin.core.rules.exceptions.NullComparisonException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogicalExpressionTest {

    private static LogicalExpression.ComparisonWrap validComparison;

    @BeforeAll
    static void setUp() {
        final var dimBuilder = new Dimension.Builder("dim");
        dimBuilder.addPartition("p1");
        dimBuilder.addPartition("p2");
        final var dim = dimBuilder.build();

        final var rel = new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, dim);
        final var fix = new PartitionExpression.Fixed(dim.getPartition("p1"));
        validComparison = new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(rel, fix));
    }

    @ParameterizedTest(name = "Creation with null in {0} should throw NullComparisonException")
    @MethodSource("provideNullCases")
    void shouldThrowOnNullComponents(final Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(NullComparisonException.class);
    }

    static Stream<Arguments> provideNullCases() {
        return Stream.of(
                Arguments.of((Runnable) () -> new LogicalExpression.And(null, validComparison)),
                Arguments.of((Runnable) () -> new LogicalExpression.And(validComparison, null)),
                Arguments.of((Runnable) () -> new LogicalExpression.Or(null, validComparison)),
                Arguments.of((Runnable) () -> new LogicalExpression.Or(validComparison, null)),
                Arguments.of((Runnable) () -> new LogicalExpression.Not(null)),
                Arguments.of((Runnable) () -> new LogicalExpression.Group(null)),
                Arguments.of((Runnable) () -> new LogicalExpression.ComparisonWrap(null))
        );
    }

    @Test
    void shouldThrowOnRedundantBinaryLogic() {
        assertThatThrownBy(() -> new LogicalExpression.And(validComparison, validComparison))
                .isInstanceOf(IllegalArgumentException.class);
    }
}