package io.github.march_plugin.core.config.classification.model;

import java.util.Objects;

public record ModuleCoordinates(String groupId, String artifactId) {

    /**
     * Builds the Coordinates.
     *
     * @param groupId the groupId of the module
     * @param artifactId the artifactId of the module
     */
    public ModuleCoordinates {
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(artifactId);
    }

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

}
