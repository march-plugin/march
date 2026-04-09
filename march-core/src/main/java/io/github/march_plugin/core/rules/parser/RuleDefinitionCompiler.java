package io.github.march_plugin.core.rules.parser;


import io.github.march_plugin.core.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.rules.model.ast.LogicalExpression;

public class RuleDefinitionCompiler {

    private final DimensionRegistry dimensionRegistry;

    /**
     * TODO .
     * @param dimensionRegistry .
     */
    public RuleDefinitionCompiler(final DimensionRegistry dimensionRegistry) {
        this.dimensionRegistry = dimensionRegistry;
    }

    /**
     * TODO .
     *
     * @param rule todo
     * @return .
     */
    public LogicalExpression compile(final String rule) {
        final var tokens = new Lexer(rule).tokenize();
        return new PolicyParser(tokens, dimensionRegistry).parse();
    }
}
