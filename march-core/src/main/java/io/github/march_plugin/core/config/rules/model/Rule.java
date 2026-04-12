package io.github.march_plugin.core.config.rules.model;

import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;

public record Rule(
        String description,
        LogicalExpression definition,
        RuleScope ruleScope
) {

    public enum RuleScope {
        GLOBAL,
        MODULE_ONLY,
        PACKAGE_ONLY
    }

}