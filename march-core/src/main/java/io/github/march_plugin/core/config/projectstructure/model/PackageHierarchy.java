package io.github.march_plugin.core.config.projectstructure.model;

import io.github.march_plugin.core.config.projectstructure.exception.EmptyPackageNameException;
import io.github.march_plugin.core.config.projectstructure.exception.IllegalPackageNameException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the path of a package.
 */
public final class PackageHierarchy {
    private final List<String> packageHierarchy;

    /**
     * Constructs the path of the package.
     *
     * @param packageHierarchy the package names of the path
     */
    public PackageHierarchy(final List<String> packageHierarchy) {
        if (packageHierarchy == null || packageHierarchy.isEmpty()) {
            throw new EmptyPackageNameException();
        }

        for (final var packageName : packageHierarchy) {
            if (packageName == null || packageName.isBlank()) {
                throw new EmptyPackageNameException();
            }
        }

        if (packageHierarchy.stream().anyMatch(p -> p.contains("."))) {
            throw new IllegalPackageNameException();
        }
        this.packageHierarchy = List.copyOf(packageHierarchy.stream().map(x -> x.replace("-", "_")).toList());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PackageHierarchy other) {
            return packageHierarchy.equals(other.packageHierarchy);
        }
        return false;
    }

    /**
     * Gets the number of packages defined in the hierarchy.
     * @return the number of packages
     */
    public int depth() {
        return packageHierarchy.size();
    }

    /**
     * Gets the package at the given index.
     * @param index the index of the package in the hierarchy
     * @return the package at the index
     */
    public String get(final int index) {
        return packageHierarchy.get(index);
    }

    /**
     * Returns the last package defined in the hierarchy.
     * @return the name of the last package
     */
    public String getSimpleName() {
        return packageHierarchy.getLast();
    }

    /**
     * Builds the path of the package hierarchy.
     *
     * @param root the root package, where the hierarchy is appended to
     * @return the path of root concatenated with the package hierarchy
     */
    public Path buildPath(final String root) {
        return Paths.get(root, packageHierarchy.toArray(new String[0]));
    }

    @Override
    public int hashCode() {
        return packageHierarchy.hashCode();
    }

    @Override
    public String toString() {
        return String.join(".", packageHierarchy);
    }

    /**
     * Builds a hierarchy by appending a package to a parent.
     *
     * @param parent the parent hierarchy
     * @param childPackage the package to append to the parent
     * @return the build child hierarchy
     */
    public static PackageHierarchy buildChild(final PackageHierarchy parent, final String childPackage) {
        final var childList = parent == null ? new ArrayList<String>() : new ArrayList<>(parent.packageHierarchy);
        childList.add(childPackage);
        return new PackageHierarchy(childList);
    }
}