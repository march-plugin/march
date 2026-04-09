package io.github.march_plugin.core.rules.parser;

import io.github.march_plugin.core.rules.exceptions.InvalidRuleDeclarationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Performs lexical analysis on policy strings to generate a sequence of tokens.
 *
 * <p>This lexer breaks down a rule string into recognizable components such as
 * logical operators, parentheses, and identifiers (e.g., source.dimension).</p>
 */
class Lexer {
    private final String input;

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(\\(|\\)|&&|\\|\\||==|!=|\\||!)|([a-zA-Z0-9._-]+)"
    );
    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]*)?$");

    /**
     * Constructs a new Lexer instance.
     *
     * @param input The raw rule string to process.
     */
    public Lexer(final String input) {
        this.input = input;
    }

    /**
     * Processes the input string and returns a list of identified tokens.
     *
     * @return A list of {@link Token} objects representing the rule logic.
     * @throws InvalidRuleDeclarationException If a token does not match the expected identifier format.
     */
    public List<Token> tokenize() {
        final var tokens = new ArrayList<Token>();
        final var matcher = TOKEN_PATTERN.matcher(input);

        while (matcher.find()) {
            final var val = matcher.group().trim();
            if (val.isEmpty()) {
                continue;
            }

            tokens.add(new Token(determineTokenType(val), val));
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private static final Map<String, TokenType> TOKEN_MAP = Map.ofEntries(
            Map.entry("(", TokenType.OPEN_PAREN),
            Map.entry(")", TokenType.CLOSE_PAREN),
            Map.entry("&&", TokenType.AND),
            Map.entry("AND", TokenType.AND),
            Map.entry("and", TokenType.AND),
            Map.entry("||", TokenType.OR),
            Map.entry("OR", TokenType.OR),
            Map.entry("or", TokenType.OR),
            Map.entry("==", TokenType.EQUALS),
            Map.entry("!=", TokenType.NOT_EQUALS),
            Map.entry("!", TokenType.NOT),
            Map.entry("NULL", TokenType.NULL),
            Map.entry("IN", TokenType.IN),
            Map.entry("in", TokenType.IN),
            Map.entry("|", TokenType.PIPE)
    );

    private TokenType determineTokenType(final String val) {
        final var type = TOKEN_MAP.get(val);
        if (type != null) {
            return type;
        }
        return resolveIdentifierOrLiteral(val);
    }

    private TokenType resolveIdentifierOrLiteral(final String val) {
        if (!IDENTIFIER_PATTERN.matcher(val).matches()) {
            throw new InvalidRuleDeclarationException(val);
        }
        if (val.startsWith("source.") || val.startsWith("target.")) {
            return TokenType.IDENTIFIER;
        }
        return TokenType.LITERAL;
    }
}
