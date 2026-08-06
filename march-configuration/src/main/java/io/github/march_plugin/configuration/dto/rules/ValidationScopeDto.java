package io.github.march_plugin.configuration.dto.rules;

import io.github.march_plugin.core.config.rules.model.Rule;

public enum ValidationScopeDto {
    global,
    module_only,
    package_only;

    /**
     * Maps DTO to core object.
     *
     * @return the mapped object
     */
    public Rule.RuleScope toRuleScope() {
        return switch (this) {
            case global -> Rule.RuleScope.GLOBAL;
            case module_only -> Rule.RuleScope.MODULE_ONLY;
            case package_only -> Rule.RuleScope.PACKAGE_ONLY;
        };
    }
}
