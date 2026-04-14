package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

public record PackageClassification(Classification classification, PackageHierarchy packageHierarchy, boolean isClassificationLeaf) {

}