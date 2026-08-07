package io.github.march_plugin.core.enforcement.dependencies;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.nio.file.Path;
import java.util.List;

/**
 * Evaluates forbidden package dependencies using ArchUnit against the compiled bytecode of the project.
 */
public class ArchUnitPackageDependencyEvaluator implements PackageDependencyEvaluator {

    private final JavaClasses javaClasses;

    /**
     * Constructs the evaluator by importing the compiled classes from the given output directories.
     *
     * @param outputDirectories the build output directories of the project's modules to scan
     */
    public ArchUnitPackageDependencyEvaluator(final List<Path> outputDirectories) {
        javaClasses = new ClassFileImporter().importPaths(outputDirectories.toArray(new Path[0]));
    }

    @Override
    public PackageDependencyEvaluationResult evaluateForbiddenDependency(final ForbiddenDependency forbiddenDependency) {
        final var sourcePackage = forbiddenDependency.source().isClassificationLeaf() ?
                forbiddenDependency.source().packageHierarchy().toString() + ".." :
                forbiddenDependency.source().packageHierarchy().toString();

        final var targetPackage = forbiddenDependency.target().isClassificationLeaf() ?
                forbiddenDependency.target().packageHierarchy().toString() + ".." :
                forbiddenDependency.target().packageHierarchy().toString();

        final var archRule = ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage(sourcePackage)
                .should().dependOnClassesThat().resideInAnyPackage(targetPackage)
                .allowEmptyShould(true);
        final var evaluationResult = archRule.evaluate(javaClasses);

        final var detail = evaluationResult.hasViolation() ? evaluationResult.getFailureReport().getDetails().get(0) : null;

        return new PackageDependencyEvaluationResult(evaluationResult.hasViolation(), detail);
    }
}
