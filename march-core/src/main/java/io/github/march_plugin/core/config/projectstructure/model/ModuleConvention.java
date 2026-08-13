package io.github.march_plugin.core.config.projectstructure.model;

public final class ModuleConvention {
    private final String groupId;
    private final String artifactId;
    private final PackageHierarchy rootPackage;

    private ModuleConvention(final String groupId, final String artifactId, final PackageHierarchy rootPackage) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.rootPackage = rootPackage;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public PackageHierarchy getRootPackage() {
        return rootPackage;
    }

    public static class Builder {
        private String groupId;
        private String artifactId;
        private PackageHierarchy rootPackage;

        /**
         * Sets the groupId.
         * @param newGroupId the groupId to set.
         * @return the builder to enable chaining
         */
        public Builder setGroupId(final String newGroupId) {
            this.groupId = newGroupId;
            return this;
        }

        /**
         * Sets the artifactId.
         * @param newArtifactId the artifactId to set.
         * @return the builder to enable chaining
         */
        public Builder setArtifactId(final String newArtifactId) {
            this.artifactId = newArtifactId;
            return this;
        }

        /**
         * Sets the rootPackage.
         * @param newRootPackage the rootPackage to set.
         * @return the builder to enable chaining
         */
        public Builder setRootPackage(final PackageHierarchy newRootPackage) {
            this.rootPackage = newRootPackage;
            return this;
        }

        /**
         * Builds the convention.
         * @return the built instance
         */
        public ModuleConvention build() {
            return new ModuleConvention(groupId, artifactId, rootPackage);
        }
    }

    /**
     * Builds a convention based on convention of parent modularity.
     * @param parent the convention of the parent modularity
     * @param child the convention of the child modularity
     * @return the built convention
     */
    public static ModuleConvention buildChild(final ModuleConvention parent, final ModuleConvention child) {
        var groupId = parent.getGroupId();
        var artifactId = parent.getArtifactId();

        if (child.groupId != null) {
            if (parent.getGroupId().isEmpty()) {
                groupId = child.groupId;
            } else {
                groupId += "." + child.groupId;
            }
        }
        if (child.artifactId != null) {
            artifactId = child.artifactId;
        }

        return new ModuleConvention(groupId, artifactId, child.rootPackage);
    }

    @Override
    public String toString() {

        final var sb = new StringBuilder();

        if (groupId != null) {
            sb.append("GroupId: ").append(groupId).append(";");
        }

        if (artifactId != null) {
            sb.append("ArtifactId: ").append(artifactId).append(";");
        }

        if (rootPackage != null) {
            sb.append("RootPackage: ").append(rootPackage).append(";");
        }

        return sb.toString();
    }
}
