package io.github.march_plugin.core.rules.parser;

import io.github.march_plugin.core.dimensions.model.Dimension;
import io.github.march_plugin.core.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.rules.exceptions.RuleSyntaxException;
import io.github.march_plugin.core.rules.model.ast.LogicalExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PolicyParserTest {

    private DimensionRegistry registry;
    private Dimension mockDim;
    private Dimension.Partition mockPart;

    @BeforeEach
    void setup() {
        registry = Mockito.mock(DimensionRegistry.class);
        mockDim = Mockito.mock(Dimension.class);
        mockPart = Mockito.mock(Dimension.Partition.class);

        when(registry.getDimension(anyString())).thenReturn(mockDim);
        when(mockDim.getPartition(anyString())).thenReturn(mockPart);
    }

    @Test
    void testPrecedenceAndGrouping() {
        final var dimA = Mockito.mock(Dimension.class);
        final var dimB = Mockito.mock(Dimension.class);
        final var dimC = Mockito.mock(Dimension.class);

        final var partA = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);
        final var partB = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);
        final var partC = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);

        when(partA.getDimension()).thenReturn(dimA);
        when(partB.getDimension()).thenReturn(dimB);
        when(partC.getDimension()).thenReturn(dimC);

        when(registry.getDimension("a")).thenReturn(dimA);
        when(dimA.getPartition("p")).thenReturn(partA);

        when(registry.getDimension("b")).thenReturn(dimB);
        when(dimB.getPartition("p")).thenReturn(partB);

        when(registry.getDimension("c")).thenReturn(dimC);
        when(dimC.getPartition("p")).thenReturn(partC);

        final var tokens = List.of(
                new Token(TokenType.IDENTIFIER, "source.a"), new Token(TokenType.EQUALS, "=="), new Token(TokenType.LITERAL, "a.p"),
                new Token(TokenType.OR, "||"),
                new Token(TokenType.IDENTIFIER, "source.b"), new Token(TokenType.EQUALS, "=="), new Token(TokenType.LITERAL, "b.p"),
                new Token(TokenType.AND, "&&"),
                new Token(TokenType.IDENTIFIER, "source.c"), new Token(TokenType.EQUALS, "=="), new Token(TokenType.LITERAL, "c.p"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        final var result = parser.parse();

        assertThat(result).isInstanceOf(LogicalExpression.Or.class);
    }

    @Test
    void testNullComparisonParsing() {
        final var tokens = List.of(
                new Token(TokenType.IDENTIFIER, "source.layer"),
                new Token(TokenType.NOT_EQUALS, "!="),
                new Token(TokenType.NULL, "NULL"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        final var result = parser.parse();

        assertThat(result).isNotNull();
    }

    @Test
    void testInOperatorParsing() {
        final var layerDim = Mockito.mock(Dimension.class);
        final var uiPart = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);
        final var apiPart = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);

        when(registry.getDimension("layer")).thenReturn(layerDim);

        when(layerDim.getPartition("ui")).thenReturn(uiPart);
        when(layerDim.getPartition("api")).thenReturn(apiPart);

        when(uiPart.getDimension()).thenReturn(layerDim);
        when(apiPart.getDimension()).thenReturn(layerDim);

        final var tokens = List.of(
                new Token(TokenType.IDENTIFIER, "target.layer"),
                new Token(TokenType.IN, "IN"),
                new Token(TokenType.LITERAL, "layer."),
                new Token(TokenType.OPEN_PAREN, "("),
                new Token(TokenType.LITERAL, "ui"),
                new Token(TokenType.PIPE, "|"),
                new Token(TokenType.LITERAL, "api"),
                new Token(TokenType.CLOSE_PAREN, ")"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        final var result = parser.parse();

        assertThat(result).isInstanceOf(LogicalExpression.ComparisonWrap.class);
    }

    @Test
    void testSyntaxErrorMissingClosingParen() {
        final var tokens = List.of(
                new Token(TokenType.OPEN_PAREN, "("),
                new Token(TokenType.IDENTIFIER, "source.a"),
                new Token(TokenType.EQUALS, "=="),
                new Token(TokenType.LITERAL, "dim.a"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);

        final var ex = assertThrows(RuleSyntaxException.class, parser::parse);

        assertThat(ex.getMessage())
                .contains("Expected CLOSE_PAREN")
                .contains(">>>EOF<<<");
    }

    @Test
    void testUnexpectedOperatorError() {
        final var tokens = List.of(
                new Token(TokenType.IDENTIFIER, "source.a"),
                new Token(TokenType.AND, "&&"),
                new Token(TokenType.IDENTIFIER, "target.b"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        final var exception = assertThrows(RuleSyntaxException.class, parser::parse);

        assertThat(exception.getMessage())
                .contains("Expected ==, !=, or IN")
                .contains(">>>&&<<<");
    }

    @Test
    void testDeeplyNestedAndNegatedLogic() {
        final var dimA = Mockito.mock(Dimension.class);
        final var dimB = Mockito.mock(Dimension.class);
        final var dimC = Mockito.mock(Dimension.class);

        final var partA = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);
        final var partB = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);
        final var partC = Mockito.mock(io.github.march_plugin.core.dimensions.model.Dimension.Partition.class);

        when(registry.getDimension("a")).thenReturn(dimA);
        when(dimA.getPartition("p")).thenReturn(partA);

        when(registry.getDimension("b")).thenReturn(dimB);
        when(dimB.getPartition("p")).thenReturn(partB);

        when(registry.getDimension("c")).thenReturn(dimC);
        when(dimC.getPartition("p")).thenReturn(partC);

        final var tokens = List.of(
                new Token(TokenType.NOT, "!"),
                new Token(TokenType.OPEN_PAREN, "("),
                new Token(TokenType.IDENTIFIER, "source.a"), new Token(TokenType.EQUALS, "=="), new Token(TokenType.LITERAL, "a.p"),
                new Token(TokenType.OR, "||"),
                new Token(TokenType.OPEN_PAREN, "("),
                new Token(TokenType.IDENTIFIER, "source.b"), new Token(TokenType.EQUALS, "=="), new Token(TokenType.LITERAL, "b.p"),
                new Token(TokenType.AND, "&&"),
                new Token(TokenType.IDENTIFIER, "source.c"), new Token(TokenType.EQUALS, "=="), new Token(TokenType.LITERAL, "c.p"),
                new Token(TokenType.CLOSE_PAREN, ")"),
                new Token(TokenType.CLOSE_PAREN, ")"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        final var result = parser.parse();

        assertThat(result)
                .isNotNull()
                .isInstanceOf(LogicalExpression.Not.class);
    }

    @Test
    void testMalformedInOperatorError() {
        final var tokens = List.of(
                new Token(TokenType.IDENTIFIER, "source.a"),
                new Token(TokenType.IN, "IN"),
                new Token(TokenType.LITERAL, "layer."),
                new Token(TokenType.LITERAL, "ui"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        final var exception = assertThrows(RuleSyntaxException.class, parser::parse);

        assertThat(exception.getMessage())
                .contains("Expected OPEN_PAREN")
                .contains("layer. >>>ui<<< EOF");
    }

    @Test
    void testEmptyParenthesesError() {
        final var tokens = List.of(
                new Token(TokenType.OPEN_PAREN, "("),
                new Token(TokenType.CLOSE_PAREN, ")"),
                new Token(TokenType.EOF, "")
        );

        final var parser = new PolicyParser(tokens, registry);
        assertThrows(RuleSyntaxException.class, parser::parse);
    }

}