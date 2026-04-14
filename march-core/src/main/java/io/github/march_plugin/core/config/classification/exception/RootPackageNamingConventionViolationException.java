package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when the root package of a module does not match the configured naming convention.
 */
public class RootPackageNamingConventionViolationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param actualRootPackage the root package of the module
     * @param expectedRootPackage the expected root package
     */
    public RootPackageNamingConventionViolationException(final String actualRootPackage, final String expectedRootPackage) {
        super("The root package '%s' does not match the naming convention '%s' defined in project structure".formatted(actualRootPackage, expectedRootPackage));
    }
}