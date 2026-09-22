package io.github.march_plugin.configuration.initializer.exception;

import io.github.march_plugin.configuration.dto.rules.RuleSetDto;
import io.github.march_plugin.core.exceptions.MarchViolationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when an explicitly requested rule set name (e.g. via {@code -Dmarch.ruleSet}) does not match
 * any rule set declared under {@code <rules>}.
 */
public class UnresolvedRuleSetException extends MarchViolationException {

    /**
     * Constructs the exception.
     *
     * @param requestedRuleSetName the requested rule set name
     * @param declaredRuleSets     all configured rule sets
     */
    public UnresolvedRuleSetException(final String requestedRuleSetName, final List<RuleSetDto> declaredRuleSets) {
        super(message(requestedRuleSetName, declaredRuleSets));
    }

    private static String message(final String requestedRuleSetName, final List<RuleSetDto> declaredRuleSets) {
        final var declaredNames = declaredRuleSets.stream().map(RuleSetDto::name).collect(Collectors.joining(", "));
        return "No rule set named '%s' is declared under <rules> (%s).".formatted(requestedRuleSetName, declaredNames);
    }
}
