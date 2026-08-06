package io.github.march_plugin.core.config.classification.model;

import java.util.Objects;

public class ModuleCoordinates {

    private final String groupId;
    private final String artifactId;

    /**
     * Builds the Coordinates.
     *
     * @param groupId the groupId of the module
     * @param artifactId the artifactId of the module
     */
    public ModuleCoordinates(final String groupId, final String artifactId) {
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(artifactId, "artifactId cannot be null");
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Gets the group id of the module.
     *
     * @return the group id
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the artifact id of the module.
     *
     * @return the artifact id
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Returns the standard Maven identifier format: groupId:artifactId.
     */
    @Override
    public String toString() {
        return String.format("%s:%s", groupId, artifactId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ModuleCoordinates otherModuleCoordinates)) {
            return false;
        }
        return Objects.equals(groupId, otherModuleCoordinates.groupId) && Objects.equals(artifactId, otherModuleCoordinates.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }
}
