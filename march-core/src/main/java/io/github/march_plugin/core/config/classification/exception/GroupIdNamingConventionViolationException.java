package io.github.march_plugin.core.config.classification.exception;

import io.github.march_plugin.core.exceptions.MarchViolationException;

/**
 * Thrown when the groupId of a module does not match the configured naming convention.
 */
public class GroupIdNamingConventionViolationException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param artifactId the artifact id of the module.
     * @param actualGroupId the actual group id of the module.
     * @param expectedGroupId the group id computed from the project structure conventions.
     */
    public GroupIdNamingConventionViolationException(final String artifactId, final String actualGroupId, final String expectedGroupId) {
        super(("Naming convention violated for module '%s': its groupId is '%s', but the project structure conventions of march config expect '%s'")
                .formatted(artifactId, actualGroupId, expectedGroupId));
    }
}