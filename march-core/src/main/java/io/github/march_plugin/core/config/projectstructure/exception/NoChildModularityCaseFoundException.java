package io.github.march_plugin.core.config.projectstructure.exception;

/**
 * Thrown when no child modularity is defined for a specific partition.
 */
public class NoChildModularityCaseFoundException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param partition the (dimension-qualified) partition that no child modularity declares a matching 'case' for
     * @param partitionName the bare name of the partition, as it would need to appear in a 'case' attribute
     * @param availableCases a comma-separated list of the case values the existing children DO declare at this
     *                       level, or an empty string if none of them declare a case
     */
    public NoChildModularityCaseFoundException(final String partition, final String partitionName, final String availableCases) {
        super(("The partition '%s' could not be resolved in the project structure of march config, because no child "
                + "modularity node declares case=\"...\" for it. Available case values at this level: %s. Either add "
                + "a new child modularity node with case=\"%s\" (possibly alongside other partitions, e.g. "
                + "case=\"%s;other\"), or widen an existing child's 'case' attribute to also include '%s'.")
                .formatted(partition,
                        availableCases.isEmpty() ? "(none — this level currently expects exactly one uncased child)" : availableCases,
                        partitionName, partitionName, partitionName));
    }
}