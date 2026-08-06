package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.ArtifactIdNamingConventionViolationException;
import io.github.march_plugin.core.config.classification.exception.GroupIdNamingConventionViolationException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;

import java.util.Objects;
import java.util.function.Function;

public final class ModuleConventionValidator {

    /**
     * Validates the conventions of a module.
     *
     * @param convention the convention of the module
     * @param replacementFunction replaces variables of the convention with classification of the module
     * @param moduleCoordinates the maven coordinates of the module to validate
     */
    public void validate(final ModuleConvention convention, final Function<String, String> replacementFunction, final ModuleCoordinates moduleCoordinates) {
        if (convention.getGroupId() != null) {
            final var expectedGroupId = new NamingPatternReplacer().replaceString(
                    convention.getGroupId(),
                    replacementFunction);

            if (!Objects.equals(moduleCoordinates.getGroupId(), expectedGroupId)) {
                throw new GroupIdNamingConventionViolationException(moduleCoordinates.getArtifactId(), moduleCoordinates.getGroupId(), expectedGroupId);
            }
        }

        if (convention.getArtifactId() != null) {
            final var expectedArtifactId = new NamingPatternReplacer().replaceString(
                    convention.getArtifactId(),
                    replacementFunction);

            if (!Objects.equals(moduleCoordinates.getArtifactId(), expectedArtifactId)) {
                throw new ArtifactIdNamingConventionViolationException(moduleCoordinates.getArtifactId(), expectedArtifactId);
            }
        }
    }
}
