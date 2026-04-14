package io.github.march_plugin.core.config.classification.exception;

/**
 * Thrown when a package of a module does not match the configured naming convention.
 */
public class PackageNamingConventionViolationException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param actualPackageName the package name of the module
     * @param expectedRootPackageName the expected package name
     */
    public PackageNamingConventionViolationException(final String actualPackageName, final String expectedRootPackageName) {
        super("The package '%s' does not match the naming convention '%s' defined in project structure".formatted(actualPackageName, expectedRootPackageName));
    }
}