package io.github.march_plugin.core.rules.parser;

import io.github.march_plugin.core.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.rules.exceptions.InvalidRuleDeclarationException;
import io.github.march_plugin.core.rules.exceptions.RuleSyntaxException;
import io.github.march_plugin.core.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.rules.model.ast.PartitionExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a sequence of tokens into a structured Abstract Syntax Tree (AST).
 *
 */
class PolicyParser {
    private final DimensionRegistry dimensionRegistry;
    private final List<Token> tokens;
    private int cursor = 0;

    /**
     * Constructs a new PolicyParser.
     *
     * @param tokens The list of tokens to parse.
     * @param dimensionRegistry The registry for validating dimensions and partitions.
     */
    public PolicyParser(final List<Token> tokens, final DimensionRegistry dimensionRegistry) {
        this.tokens = List.copyOf(tokens);
        this.dimensionRegistry = dimensionRegistry;
    }

    /**
     * Starts the parsing process to produce a LogicalExpression.
     *
     * @return The root of the parsed Abstract Syntax Tree.
     */
    public LogicalExpression parse() {
        final var expr = parseOr();
        eat(TokenType.EOF);
        return expr;
    }

    private LogicalExpression parseOr() {
        var left = parseAnd();
        while (peek().type() == TokenType.OR) {
            eat(TokenType.OR);
            left = new LogicalExpression.Or(left, parseAnd());
        }
        return left;
    }

    private LogicalExpression parseAnd() {
        var left = parseComparisonWrap();
        while (peek().type() == TokenType.AND) {
            eat(TokenType.AND);
            left = new LogicalExpression.And(left, parseComparisonWrap());
        }
        return left;
    }

    private LogicalExpression parseComparisonWrap() {
        if (peek().type() == TokenType.NOT) {
            eat(TokenType.NOT);
            return new LogicalExpression.Not(parseComparisonWrap());
        }

        if (peek().type() == TokenType.OPEN_PAREN) {
            eat(TokenType.OPEN_PAREN);
            final var expr = parseOr();
            eat(TokenType.CLOSE_PAREN);
            return new LogicalExpression.Group(expr);
        }

        final var left = parsePartitionExpression();
        final var operator = peek();

        if (operator.type() == TokenType.EQUALS) {
            eat(TokenType.EQUALS);
            return new LogicalExpression.ComparisonWrap(new ComparisonExpression.Equal(left, parsePartitionExpression()));
        } else if (operator.type() == TokenType.NOT_EQUALS) {
            eat(TokenType.NOT_EQUALS);
            return new LogicalExpression.ComparisonWrap(new ComparisonExpression.NotEqual(left, parsePartitionExpression()));
        } else if (operator.type() == TokenType.IN) {
            eat(TokenType.IN);
            final var prefix = eat(TokenType.LITERAL).value();

            eat(TokenType.OPEN_PAREN);
            final var options = new ArrayList<PartitionExpression.Fixed>();

            options.add((PartitionExpression.Fixed) resolveInOption(prefix, eat(peek().type()).value()));

            while (peek().type() == TokenType.PIPE) {
                eat(TokenType.PIPE);
                options.add((PartitionExpression.Fixed) resolveInOption(prefix, eat(peek().type()).value()));
            }

            eat(TokenType.CLOSE_PAREN);
            return new LogicalExpression.ComparisonWrap(new ComparisonExpression.In((PartitionExpression.Relative) left, options));
        }

        throw RuleSyntaxException.of("==, !=, or IN", operator.value(), cursor, getTokenValues());
    }


    private PartitionExpression resolveInOption(final String prefix, final String value) {
        final var fullPath = prefix + value;
        final var parts = fullPath.split("\\.");
        if (parts.length != 2) {
            throw new InvalidRuleDeclarationException(fullPath);
        }
        return new PartitionExpression.Fixed(dimensionRegistry.getDimension(parts[0]).getPartition(parts[1]));
    }

    private PartitionExpression parsePartitionExpression() {
        final var token = peek();
        if (token.type() == TokenType.IDENTIFIER) {
            return createRelative(eat(TokenType.IDENTIFIER).value());
        } else if (token.type() == TokenType.LITERAL) {
            final var val = eat(TokenType.LITERAL).value();

            final var parts = val.split("\\.");

            if (parts.length != 2) {
                throw new InvalidRuleDeclarationException(val);
            }

            return new PartitionExpression.Fixed(dimensionRegistry.getDimension(parts[0]).getPartition(parts[1]));
        } else if (token.type() == TokenType.NULL) {
            eat(TokenType.NULL);
            return new PartitionExpression.Null();
        }

        throw RuleSyntaxException.of("Identifier, Literal, or NULL", token.value(), cursor, getTokenValues());
    }

    private PartitionExpression.Relative createRelative(final String value) {
        final var parts = value.split("\\.");
        if (parts.length != 2) {
            throw new InvalidRuleDeclarationException(value);
        }

        final var side = switch (parts[0]) {
            case "source" -> PartitionExpression.Relative.Side.SOURCE;
            case "target" -> PartitionExpression.Relative.Side.TARGET;
            default -> throw new InvalidRuleDeclarationException(value);
        };

        final var dimension = dimensionRegistry.getDimension(parts[1]);

        return new PartitionExpression.Relative(side, dimension);
    }

    private Token peek() {
        return tokens.get(cursor);
    }

    private Token eat(final TokenType type) {
        final var current = peek();
        if (current.type() == type) {
            cursor++;
            return current;
        }
        throw RuleSyntaxException.of(type.name(), current.value(), cursor, getTokenValues());
    }

    private List<String> getTokenValues() {
        return tokens.stream().map(Token::value).toList();
    }
}