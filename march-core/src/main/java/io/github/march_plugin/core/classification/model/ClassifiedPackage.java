package io.github.march_plugin.core.classification.model;

public record ClassifiedPackage(Classification classification, PackageHierarchy packageHierarchy, boolean isClassificationLeaf) {

}