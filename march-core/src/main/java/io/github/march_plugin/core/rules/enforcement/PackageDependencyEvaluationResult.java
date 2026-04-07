package io.github.march_plugin.core.rules.enforcement;

public record PackageDependencyEvaluationResult(
        boolean containsViolation,
        String detail
) {
}
