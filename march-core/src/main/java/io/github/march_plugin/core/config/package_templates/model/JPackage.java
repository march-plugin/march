package io.github.march_plugin.core.config.package_templates.model;

import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

import java.util.List;

public record JPackage(
        PackageHierarchy packageHierarchy,
        String partition,
        Boolean optional,
        List<JPackage> children
) {
}
