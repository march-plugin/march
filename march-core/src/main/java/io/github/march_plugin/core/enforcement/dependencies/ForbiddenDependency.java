package io.github.march_plugin.core.enforcement.dependencies;

import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.rules.model.Rule;

public record ForbiddenDependency(ClassifiedPackage source, ClassifiedPackage target, Rule rule) {
}