package io.github.march_plugin.core.enforcement.dependencies;

/**
 * Configures which dependency-declaration best practices {@link ModuleDependencyEnforcer} enforces.
 *
 * @param requireManagedVersion managed dependencies must define a version
 * @param forbidInlineVersion dependencies must not define an inline version
 * @param forbidInlineScope dependencies must not define an inline scope
 * @param forbidExclusions dependencies must not define exclusions
 * @param requireVersionProperty every declared version must be a property reference ({@code ${...}}), not a literal
 */
public record StaticEnforcementConfig(
        boolean requireManagedVersion,
        boolean forbidInlineVersion,
        boolean forbidInlineScope,
        boolean forbidExclusions,
        boolean requireVersionProperty
) {

    /**
     * The default configuration: every check enabled except forbidExclusions.
     *
     * @return the default configuration
     */
    public static StaticEnforcementConfig defaults() {
        return new StaticEnforcementConfig(true, true, true, false, true);
    }
}
