package io.github.march_plugin.core.enforcement.dependencies;

public interface PackageDependencyEvaluator {

    /**
     * Evaluates if a forbidden package dependency exists.
     *
     * @param forbiddenDependency the dependency that is forbidden
     * @return the evaluation result
     */
    PackageDependencyEvaluationResult evaluateForbiddenDependency(final ForbiddenDependency forbiddenDependency);
}
