package io.github.march_plugin.core.rules.parser;

import io.github.march_plugin.core.rules.exceptions.InvalidRuleDeclarationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerTest {

    @Test
    void testBasicTokenization() {
        final var lexer = new Lexer("source.layer == layer.db && target.sub != NULL");
        final var tokens = lexer.tokenize();

        final var expectedTypes = List.of(
                TokenType.IDENTIFIER,
                TokenType.EQUALS,
                TokenType.LITERAL,
                TokenType.AND,
                TokenType.IDENTIFIER,
                TokenType.NOT_EQUALS,
                TokenType.NULL,
                TokenType.EOF
        );

        assertThat(tokens).hasSameSizeAs(expectedTypes);

        for (var i = 0; i < expectedTypes.size(); i++) {
            final var actual = tokens.get(i);
            final var expected = expectedTypes.get(i);

            assertThat(actual.type()).isEqualTo(expected);
        }

        assertThat(tokens.get(0).value()).isEqualTo("source.layer");
        assertThat(tokens.get(2).value()).isEqualTo("layer.db");
        assertThat(tokens.get(4).value()).isEqualTo("target.sub");
    }

    @Test
    void testCaseInsensitivityAndAliases() {
        final var input = "source.dim IN dim.(a|b) AND target.dim == NULL OR !source.x == NULL";
        final var lexer = new Lexer(input);
        final var tokens = lexer.tokenize();

        final var expectedTypes = List.of(
                TokenType.IDENTIFIER,
                TokenType.IN,
                TokenType.LITERAL,
                TokenType.OPEN_PAREN,
                TokenType.LITERAL,
                TokenType.PIPE,
                TokenType.LITERAL,
                TokenType.CLOSE_PAREN,
                TokenType.AND,
                TokenType.IDENTIFIER,
                TokenType.EQUALS,
                TokenType.NULL,
                TokenType.OR,
                TokenType.NOT,
                TokenType.IDENTIFIER,
                TokenType.EQUALS,
                TokenType.NULL,
                TokenType.EOF
        );

        assertThat(tokens).hasSameSizeAs(expectedTypes);

        for (var i = 0; i < expectedTypes.size(); i++) {
            assertThat(tokens.get(i).type()).isEqualTo(expectedTypes.get(i));
        }
    }

    @Test
    void testInvalidIdentifierFormat() {
        final var lexer = new Lexer("source.dim.invalid");
        assertThrows(InvalidRuleDeclarationException.class, lexer::tokenize);
    }

    @Test
    void testInvalidCharacters() {
        final var lexer = new Lexer("source..dimension");
        assertThrows(InvalidRuleDeclarationException.class, lexer::tokenize);
    }

    @Test
    void testLexerWhitespaceAndComplexInput() {
        final var lexer = new Lexer("source.layer\n==  layer.api\t&& !target.sub");
        final var tokens = lexer.tokenize();

        assertThat(tokens.get(0).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.EQUALS);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.LITERAL);
        assertThat(tokens.get(3).type()).isEqualTo(TokenType.AND);
        assertThat(tokens.get(4).type()).isEqualTo(TokenType.NOT);
    }
}