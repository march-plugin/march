package io.github.march_plugin.configuration.initializer.exception;

import io.github.march_plugin.configuration.dto.rules.RuleSetDto;
import io.github.march_plugin.core.exceptions.MarchViolationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when {@code <rules>} declares one or more rule sets but setting fo active ruleset is missing,
 * or names a rule set that is not actually declared.
 */
public class UnresolvedActiveRuleSetException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param activeRuleSetName the configured rule set name
     * @param declaredRuleSets  all configured rule sets
     */
    public UnresolvedActiveRuleSetException(final String activeRuleSetName, final List<RuleSetDto> declaredRuleSets) {
        super(message(activeRuleSetName, declaredRuleSets));
    }

    private static String message(final String activeRuleSetName, final List<RuleSetDto> declaredRuleSets) {
        final var declaredNames = declaredRuleSets.stream().map(RuleSetDto::name).collect(Collectors.joining(", "));
        if (activeRuleSetName == null) {
            return "settings/activeRuleSet must name one of the declared rule sets (%s), since <rules> declares at least one.".formatted(declaredNames);
        }
        return "settings/activeRuleSet '%s' does not match any declared rule set (%s).".formatted(activeRuleSetName, declaredNames);
    }
}
