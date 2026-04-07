package io.github.march_plugin.core.rules.enforcement;

import io.github.march_plugin.core.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.rules.model.Rule;

public record ForbiddenDependency(ClassifiedPackage source, ClassifiedPackage target, Rule rule) {
}