package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

public record ClassifiedPackage(Classification classification, PackageHierarchy packageHierarchy, boolean isClassificationLeaf) {

}