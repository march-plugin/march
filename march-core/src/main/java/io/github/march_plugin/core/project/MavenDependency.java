package io.github.march_plugin.core.project;

import io.github.march_plugin.core.config.classification.model.Classification;

public record MavenDependency(Classification source, Classification target, String description) {

}