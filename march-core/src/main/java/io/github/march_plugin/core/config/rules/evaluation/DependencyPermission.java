package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;

import java.util.List;

public sealed interface DependencyPermission {
    record Forbidden() implements DependencyPermission {
    }

    record Allowed() implements DependencyPermission {
    }

    record PartiallyAllowed(List<LogicalExpression> allowedCases) implements DependencyPermission {
    }
}