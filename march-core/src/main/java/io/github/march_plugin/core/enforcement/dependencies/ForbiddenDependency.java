package io.github.march_plugin.core.enforcement.dependencies;

import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.rules.model.Rule;

public record ForbiddenDependency(PackageClassification source, PackageClassification target, Rule rule) {
}