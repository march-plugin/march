package io.github.march_plugin.core.config.rules.parser;

import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;

/**
 * Orchestrates the conversion of raw string rules into a structured Abstract Syntax Tree (AST).
 */
public class RuleDefinitionCompiler {

    private final DimensionRegistry dimensionRegistry;

    /**
     * Constructs the compiler.
     *
     * @param dimensionRegistry the registry for dimension and partition validation.
     */
    public RuleDefinitionCompiler(final DimensionRegistry dimensionRegistry) {
        this.dimensionRegistry = dimensionRegistry;
    }

    /**
     * Compiles the raw rule string into a structured Abstract Syntax Tree (AST).
     *
     * @param rule the raw rule string to be processed.
     * @return the root node of the generated AST.
     */
    public LogicalExpression compile(final String rule) {
        final var tokens = new Lexer(rule).tokenize();
        return new PolicyParser(tokens, dimensionRegistry).parse();
    }
}