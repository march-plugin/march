package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;

import java.util.List;

public sealed interface DependencyPermission {
    record Forbidden() implements DependencyPermission {
    }

    record Allowed() implements DependencyPermission {
    }

    record PartiallyAllowed(List<EvaluatedLogicalExpression> allowedCases) implements DependencyPermission {
    }
}