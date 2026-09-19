package io.github.march_plugin.core.project;

import io.github.march_plugin.core.config.classification.model.Classification;

public record MavenDependency(Classification source, Classification target, boolean sourceIsLeaf, boolean targetIsLeaf, String description) {

}