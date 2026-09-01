package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the artifactId of a module does not match the configured naming convention.
 */
public class ArtifactIdNamingConventionViolationException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param actualArtifactId the actual artifact id of the module.
     * @param expectedArtifactId the artifact id computed from the project structure conventions.
     */
    public ArtifactIdNamingConventionViolationException(final String actualArtifactId, final String expectedArtifactId) {
        super(("Naming convention violated: module '%s' has artifactId '%s', but the project structure of march config expects artifactId '%s'. "
                + "Either rename the module, or fix the 'artifactId' attribute/placeholder on the modularity node.")
                .formatted(actualArtifactId, actualArtifactId, expectedArtifactId));
    }
}