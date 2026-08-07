package io.github.march_plugin.core.enforcement.dependencies.exception;

/**
 * Thrown when a dependency has a forbidden exclusion.
 */
public class ForbiddenExclusionException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dependencyDescription the dependency containing the forbidden exclusion.
     * @param excludedModule the excluded module.
     */
    public ForbiddenExclusionException(final String dependencyDescription, final String excludedModule) {
        super("Dependency '" + dependencyDescription + "' must not exclude internal dependency '" + excludedModule + "'.");
    }
}