package io.github.march_plugin.core.rules.model;

import io.github.march_plugin.core.classification.model.Classification;

public record MavenDependency(Classification source, Classification target, String description) {

}