package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when the groupId of a module does not match the configured naming convention.
 */
public class GroupIdNamingConventionViolationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param artifactId the artifact id of the module.
     * @param groupId the group id of the module.
     * @param expectedName the expected group id.
     */
    public GroupIdNamingConventionViolationException(final String artifactId, final String groupId, final String expectedName) {
        super("Naming Convention violated: Expected artifact '" + artifactId + "' with groupId '" + groupId + "' to have groupId '" + expectedName + "'");
    }
}