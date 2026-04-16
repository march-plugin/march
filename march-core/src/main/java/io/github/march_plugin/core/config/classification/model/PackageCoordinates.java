package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

public record PackageCoordinates(
        ModuleCoordinates moduleCoordinates,
        PackageHierarchy packageHierarchy
) {

    @Override
    public String toString() {
        return moduleCoordinates.toString() + ":" + packageHierarchy.toString();
    }
}
