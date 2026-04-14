package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.PackageNamingConventionViolationException;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;

import java.util.Objects;
import java.util.function.Function;

public final class PackageModularityConventionValidator {

    /**
     * Validates the naming convention of a package.
     *
     * @param convention the package convention containing placeholders
     * @param replacementFunction a function providing the replacement for a certain placeholder
     * @param actualPackageName the name of the package
     */
    public void validate(final PackageConvention convention, final Function<String, String> replacementFunction, final String actualPackageName) {
        if (convention.packageName() != null) {
            final var expectedPackageName = new NamingPatternReplacer().replaceString(
                    convention.packageName(),
                    replacementFunction);

            if (!Objects.equals(actualPackageName, expectedPackageName)) {
                throw new PackageNamingConventionViolationException(actualPackageName, expectedPackageName);
            }
        }
    }
}
