package io.github.march_plugin.core.enforcement.rules.exceptions;

/**
 * Thrown when a package dependency violates a specific rule.
 */
public class PackageDependencyForbiddenException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param dependency the description of the dependency
     * @param rule the description of the rule
     * @param detail description of the violation
     */
    public PackageDependencyForbiddenException(final String dependency, final String rule, final String detail) {
        super("Dependency '" + dependency + "' is forbidden by rule '" + rule + "'. " + detail);
    }
}