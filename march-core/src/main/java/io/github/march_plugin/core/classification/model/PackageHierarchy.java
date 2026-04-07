package io.github.march_plugin.core.classification.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents the path of a package.
 * @param packageHierarchy the package names of the path
 */
public record PackageHierarchy(List<String> packageHierarchy) {

    /**
     * Constructs the path of the package.
     *
     * @param packageHierarchy the package names of the path
     * @throws NullPointerException if packageHierarchy is null
     */
    public PackageHierarchy {
        Objects.requireNonNull(packageHierarchy, "packageHierarchy cannot be null");
        packageHierarchy = List.copyOf(packageHierarchy);
    }

    /**
     * Returns the package path joined by dots.
     *
     * @return the dot-separated package string
     */
    @Override
    public String toString() {
        return String.join(".", packageHierarchy);
    }
}