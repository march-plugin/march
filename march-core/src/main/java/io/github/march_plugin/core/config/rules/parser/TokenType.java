package io.github.march_plugin.core.config.rules.parser;

enum TokenType {
    IDENTIFIER,   // source.domain, target.layer
    LITERAL,      // presentation, user, dto
    EQUALS,       // ==
    NOT_EQUALS,   // !=
    AND,          // &&
    OR,           // ||
    OPEN_PAREN,   // (
    CLOSE_PAREN,  // )
    NULL,         // NULL
    NOT,          // !
    IN,           // IN
    PIPE,         // |
    EOF
}