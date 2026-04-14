package io.github.march_plugin.core.config.classification.model;

import io.github.march_plugin.core.config.classification.exception.RootPackageNamingConventionViolationException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RootPackageConventionValidatorTest {
    private final RootPackageConventionValidator validator = new RootPackageConventionValidator();

    @Test
    void shouldHandleHyphenToUnderscoreTransformation() {
        final var convention = mock(ModuleConvention.class);
        when(convention.getRootPackage()).thenReturn(new PackageHierarchy(List.of("my-package")));

        final var actual = new PackageHierarchy(List.of("my_package"));

        assertThatCode(() -> validator.validate(convention, k -> k, actual))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenRootPackageMismatch() {
        final var convention = mock(ModuleConvention.class);
        when(convention.getRootPackage()).thenReturn(new PackageHierarchy(List.of("expected")));
        final var actual = new PackageHierarchy(List.of("actual"));

        assertThatThrownBy(() -> validator.validate(convention, k -> k, actual))
                .isInstanceOf(RootPackageNamingConventionViolationException.class);
    }
}