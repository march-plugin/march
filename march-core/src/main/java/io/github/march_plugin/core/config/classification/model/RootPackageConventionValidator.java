package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.RootPackageNamingConventionViolationException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;

import java.util.Objects;
import java.util.function.Function;

public final class RootPackageConventionValidator {

    /**
     * Validates the naming convention of the root package of a module.
     *
     * @param convention the convention containing the root package placeholders
     * @param replacementFunction a function providing the replacement for a certain placeholder
     * @param actualRootPackage the name of the module
     */
    public void validate(final ModuleConvention convention, final Function<String, String> replacementFunction, final PackageHierarchy actualRootPackage) {
        if (convention.getRootPackage() != null) {
            final var expectedRootPackage = new NamingPatternReplacer().replaceString(
                    convention.getRootPackage().toString(),
                    replacementFunction)
                    .replace("-", "_");

            final var actualRootPackageString = actualRootPackage == null ? null : actualRootPackage.toString();
            if (!Objects.equals(actualRootPackageString, expectedRootPackage)) {
                throw new RootPackageNamingConventionViolationException(actualRootPackageString, expectedRootPackage);
            }
        }
    }
}
