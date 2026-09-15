package io.github.march_plugin.core.config.rules.redundancy;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

/**
 * Builds a SAT4J solver preloaded with a fixed set of clauses.
 */
final class SatSolverBuilder {

    private final SatVariablePool variables;

    SatSolverBuilder(final SatVariablePool variables) {
        this.variables = variables;
    }

    /**
     * Builds a SAT4J solver preloaded with this instance's clauses.
     */
    public ISolver build() {
        final var solver = SolverFactory.newDefault();
        try {
            solver.newVar(Math.max(variables.variableCount(), 1));
            solver.setExpectedNumberOfClauses(variables.clauses().size());
            for (final var clause : variables.clauses()) {
                solver.addClause(new VecInt(clause));
            }
        } catch (final ContradictionException e) {
            throw new IllegalStateException("Unexpected contradiction while encoding rules for SAT analysis", e);
        }
        return solver;
    }
}
