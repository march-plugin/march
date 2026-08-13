package io.github.march_plugin.core.config.projectstructure.model;

public record PackageConvention(String packageName) {


    @Override
    public String toString() {
        return "PackageName: " + packageName;
    }
}
