package io.github.march_plugin.core.enforcement.dependencies;

public record PackageDependencyEvaluationResult(
        boolean containsViolation,
        String detail
) {
}
