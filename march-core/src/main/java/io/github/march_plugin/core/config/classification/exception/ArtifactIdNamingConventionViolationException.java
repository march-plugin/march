package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when the artifactId of a module does not match the configured naming convention.
 */
public class ArtifactIdNamingConventionViolationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param artifactId the artifact id of the module.
     * @param expectedName the expected artifact id.
     */
    public ArtifactIdNamingConventionViolationException(final String artifactId, final String expectedName) {
        super("Naming Convention violated: Expected artifact '" + artifactId + "' to have artifactId '" + expectedName + "'");
    }
}