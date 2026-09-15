package io.github.march_plugin.core.config.rules.redundancy;

import java.util.ArrayList;
import java.util.List;

final class SatVariablePool {

    private final List<int[]> clauses = new ArrayList<>();
    private int variableCount;

    int allocate() {
        return ++variableCount;
    }

    int variableCount() {
        return variableCount;
    }

    void addClause(final int... literals) {
        clauses.add(literals);
    }

    List<int[]> clauses() {
        return clauses;
    }
}
