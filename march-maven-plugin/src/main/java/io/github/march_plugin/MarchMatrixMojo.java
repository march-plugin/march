package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.configuration.initializer.RuleRegistryInitializer;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.analysis.DimensionGranularityFinder;
import io.github.march_plugin.core.config.projectstructure.analysis.PossibleCombinationsFinder;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.RuleStrategyResolver;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.evaluation.DependencyPermission;
import io.github.march_plugin.core.config.rules.evaluation.ast.EvaluatedLogicalExpression;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;
import io.github.march_plugin.core.exceptions.MarchViolationException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prints a dependency permission matrix for an abstract set of dimension combinations.
 *
 * <p>For each combination of the requested dimensions' partitions, every rule is partially evaluated against
 * every other combination as source and target, using the same three-valued reduction ({@code Allowed}/
 * {@code Forbidden}/{@code PartiallyAllowed}) used at build time. This surfaces dependencies that a
 * configuration structurally allows or forbids across the full cross product of classifications, including
 * ones no currently existing module or package happens to exercise.</p>
 *
 * <p>A rule's {@code <scope>} (module_only/package_only/GLOBAL) matters here just as it does at build time:
 * a module_only rule was only ever meant to gate the coarse pom.xml dependency edge, not a package-level
 * bytecode dependency, and vice versa. Requesting a dimension that is only ever declared inside a
 * {@code <packageModularity>} node makes this a package-level question, so module_only rules are excluded;
 * if every requested dimension only ever appears on {@code <modularity>} module nodes, it defaults to a
 * module-level question instead, excluding package_only rules. {@code march.matrixScope} overrides this
 * auto-detection explicitly.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * mvn march:matrix
 * mvn march:matrix -Dclassifications="{domain;layer}"
 * mvn march:matrix -Dclassifications="{domain(article;order);layer(api;impl)}" -Dmarch.columnWidth=10
 * mvn march:matrix -Dclassifications="{domain;module_abstraction}" -Dmarch.matrixScope=module
 * }</pre>
 */
@Mojo(name = "matrix", aggregator = true)
public class MarchMatrixMojo extends AbstractMojo {

    /**
     * Which build-time enforcement granularity a matrix query represents, mirroring {@link Rule.RuleScope}:
     * a {@code MODULE} query only considers {@code GLOBAL} and {@code MODULE_ONLY} rules (like the pom.xml
     * dependency check), a {@code PACKAGE} query only considers {@code GLOBAL} and {@code PACKAGE_ONLY}
     * rules (like the bytecode check).
     */
    enum MatrixScope {
        MODULE,
        PACKAGE
    }

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "march.configFile", defaultValue = "${project.basedir}/march-config.xml")
    private File configFile;

    /**
     * Which dimensions and partitions to build the matrix from, e.g. {@code "{domain(article;order);layer}"}.
     * When omitted, every configured dimension is used with all of its partitions.
     */
    @Parameter(property = "classifications")
    private String matrixInput;

    /**
     * Number of characters shown per column before a label is truncated. Partition names that share this
     * many leading characters (e.g. {@code adapterIn}/{@code adapterOut} at the default width of 5) become
     * indistinguishable in the table, so callers with long or similar partition names should raise this.
     */
    @Parameter(property = "march.columnWidth", defaultValue = "5")
    private int maxPartitionCharCount;

    /**
     * Forces the matrix to be evaluated as {@code "module"} or {@code "package"} scope instead of
     * auto-detecting from the requested dimensions. See the class documentation for what each means.
     */
    @Parameter(property = "march.matrixScope")
    private String matrixScope;

    private RuleStrategyResolver ruleStrategyResolver;

    private final static int firstColWidth = 20;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.isExecutionRoot()) {
            return;
        }

        validateColumnWidth(maxPartitionCharCount);

        try {
            final var marchConfigDto = new MarchConfigFileReader(configFile).readConfig();

            final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
            final var ruleRegistry = new RuleRegistryInitializer(new RuleDefinitionCompiler(dimensionRegistry)).build(marchConfigDto.rules());
            final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());

            final var partitions = new MatrixClassificationParser().parse(matrixInput, dimensionRegistry);
            final var flatPartitions = partitions
                    .stream()
                    .flatMap(Set::stream)
                    .toList();
            final var orderedDimensions = partitions
                    .stream()
                    .map(x -> x.stream().findFirst().get().getDimension())
                    .distinct()
                    .toList();
            final var dimensions = new HashSet<>(orderedDimensions);
            final var combinations = new PossibleCombinationsFinder().findCombinations(projectStructureRoot, dimensions);
            final var filteredCombinations = combinations
                    .stream()
                    .filter(flatPartitions::containsAll)
                    .map(set -> orderPartitions(set, orderedDimensions))
                    .sorted(MarchMatrixMojo::compareCombinations)
                    .toList();

            if (filteredCombinations.isEmpty()) {
                throw new MojoExecutionException(noCombinationMessage(dimensions));
            }

            final var packageOnlyDimensions = new DimensionGranularityFinder().findPackageOnlyDimensions(projectStructureRoot, dimensions);
            final var resolvedScope = resolveMatrixScope(matrixScope, packageOnlyDimensions);
            final var scopedRuleRegistry = scopedRuleRegistry(ruleRegistry, resolvedScope);

            ruleStrategyResolver = new RuleStrategyResolver(scopedRuleRegistry.getRuleStrategy(), scopedRuleRegistry.getScopeStrategy());
            renderTable(dimensionRegistry, projectStructureRoot, scopedRuleRegistry, resolvedScope, filteredCombinations);
        } catch (final MarchViolationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    static MatrixScope resolveMatrixScope(final String requestedScope, final Set<Dimension> packageOnlyDimensions) throws MojoExecutionException {
        if (requestedScope == null || requestedScope.isEmpty()) {
            return packageOnlyDimensions.isEmpty() ? MatrixScope.MODULE : MatrixScope.PACKAGE;
        }
        return switch (requestedScope) {
            case "module" -> MatrixScope.MODULE;
            case "package" -> MatrixScope.PACKAGE;
            default -> throw new MojoExecutionException("march.matrixScope must be 'module' or 'package', was: " + requestedScope);
        };
    }

    static RuleRegistry scopedRuleRegistry(final RuleRegistry ruleRegistry, final MatrixScope scope) {
        final var excludedScope = scope == MatrixScope.MODULE ? Rule.RuleScope.PACKAGE_ONLY : Rule.RuleScope.MODULE_ONLY;
        final var builder = new RuleRegistry.Builder();
        builder.setRuleStrategy(ruleRegistry.getRuleStrategy());
        builder.setScopeStrategy(ruleRegistry.getScopeStrategy());
        ruleRegistry.getRules().stream()
                .filter(r -> !r.ruleScope().equals(excludedScope))
                .forEach(builder::addRule);
        return builder.build();
    }

    static void validateColumnWidth(final int columnWidth) throws MojoExecutionException {
        if (columnWidth < 1) {
            throw new MojoExecutionException("march.columnWidth must be at least 1");
        }
    }

    static List<Dimension.Partition> orderPartitions(final Set<Dimension.Partition> combination, final List<Dimension> dimensionOrder) {
        return dimensionOrder.stream()
                .map(dimension -> combination.stream().filter(p -> p.getDimension().equals(dimension)).findFirst().orElseThrow())
                .toList();
    }

    static int compareCombinations(final List<Dimension.Partition> first, final List<Dimension.Partition> second) {
        for (var i = 0; i < first.size(); i++) {
            final var comparison = compareByDeclaredOrder(first.get(i), second.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    static int compareByDeclaredOrder(final Dimension.Partition first, final Dimension.Partition second) {
        final var declaredOrder = new ArrayList<>(first.getDimension().getPartitions());
        return Integer.compare(declaredOrder.indexOf(first), declaredOrder.indexOf(second));
    }

    static String noCombinationMessage(final Set<Dimension> dimensions) {
        return "No combination of the requested dimensions is structurally "
                + "possible: " + dimensions.stream().map(Dimension::getName).sorted().collect(Collectors.joining(", "))
                + ". Dimensions that only occur on different branches of <projectStructure> (e.g. one "
                + "used only under util modules, another only under domain modules) can never co-occur "
                + "on the same module or package. Narrow the set with -Dclassifications=\"{dim1;dim2}\".";
    }

    private int fullLineWith;

    private void printLine() {
        getLog().info("-".repeat(fullLineWith));
    }

    private void printDottedLine() {
        getLog().info(".".repeat(fullLineWith));
    }

    private void renderTable(final DimensionRegistry dimensionRegistry, final ModuleModularity projectStructureRoot, final RuleRegistry ruleRegistry, final MatrixScope resolvedScope, final List<List<Dimension.Partition>> combinations) {
        fullLineWith = firstColWidth + (combinations.size() * (maxPartitionCharCount + 1) + firstColWidth);

        getLog().info("");
        getLog().info(MessageUtils.buffer().strong("Dependency Matrix (Cross Product) — " + resolvedScope + "-level rules").build());

        printSources(combinations, "Target \\ Source", "Source / Target");
        printDottedLine();

        final var targetToPermissionMap = getTargetToPermissionsMap(dimensionRegistry, projectStructureRoot, ruleRegistry, combinations);
        final var partiallyAllowedTrees = printDataRows(targetToPermissionMap);

        printSources(combinations, "Target / Source", "Source \\ Target");
        printLine();

        for (final var partiallyAllowedTree : partiallyAllowedTrees.entrySet()) {
            getLog().info(partiallyAllowedTree.getValue() + ": " + partiallyAllowedTree.getKey());
        }
        printLine();
    }

    private void printSources(final List<List<Dimension.Partition>> combinations, final String leftDescription, final String rightDescription) {
        final var depth = combinations.get(0).size();
        for (var d = 0; d < depth; d++) {
            final var line = new StringBuilder();
            line.append(String.format("%-" + firstColWidth + "s", (d == depth - 1) ? leftDescription : ""));

            for (final var combo : combinations) {
                line.append(String.format("|%-" + maxPartitionCharCount + "s", truncate(combo.get(d).getName(), maxPartitionCharCount)));
            }
            line.append("|");
            line.append(String.format("%-" + firstColWidth + "s", (d == depth - 1) ? " " + rightDescription : ""));
            getLog().info(line.toString());
        }
    }

    private Map<List<Dimension.Partition>, List<DependencyPermission>> getTargetToPermissionsMap(final DimensionRegistry dimensionRegistry, final ModuleModularity projectStructureRoot, final RuleRegistry ruleRegistry, final List<List<Dimension.Partition>> combinations) {
        final var result = new LinkedHashMap<List<Dimension.Partition>, List<DependencyPermission>>();

        for (final var target : combinations) {
            final var permissions = new ArrayList<DependencyPermission>();
            for (final var source : combinations) {
                permissions.add(ruleStrategyResolver.getDependencyPermissionEvaluator(projectStructureRoot).reduce(ruleRegistry, dimensionRegistry, new HashSet<>(source), new HashSet<>(target)));
            }
            result.put(target, permissions);
        }
        return result;
    }

    private Map<EvaluatedLogicalExpression, Character> printDataRows(final Map<List<Dimension.Partition>, List<DependencyPermission>> targetToPermissionMap) {
        final var partiallyAllowedTrees = new HashMap<EvaluatedLogicalExpression, Character>();

        for (final var target : targetToPermissionMap.entrySet()) {
            final var label = target.getKey().stream()
                    .map(p -> truncate(p.getName(), maxPartitionCharCount))
                    .collect(Collectors.joining("/"));
            final var row = new StringBuilder(String.format("%-" + firstColWidth + "s", truncate(label, firstColWidth)));

            for (final var source : target.getValue()) {

                if (source instanceof DependencyPermission.Allowed) {
                    row.append("|").append(center("OK", maxPartitionCharCount));
                } else if (source instanceof DependencyPermission.Forbidden) {
                    row.append("|").append(center("", maxPartitionCharCount));
                } else if (source instanceof DependencyPermission.PartiallyAllowed partiallyAllowed) {
                    final var ids = new HashSet<Character>();
                    for (final var allowedCase : partiallyAllowed.allowedCases()) {
                        if (partiallyAllowedTrees.containsKey(allowedCase)) {
                            ids.add(partiallyAllowedTrees.get(allowedCase));
                        } else {
                            final var id = (char) ('A' + partiallyAllowedTrees.size());
                            partiallyAllowedTrees.put(allowedCase, id);
                            ids.add(id);
                        }
                    }

                    final var idString = ids.stream()
                            .sorted()
                            .limit(maxPartitionCharCount)
                            .map(Object::toString)
                            .collect(Collectors.joining(""));
                    row.append("|").append(center(idString, maxPartitionCharCount));
                }
            }
            row.append("| ").append(String.format("%-" + firstColWidth + "s", truncate(label, firstColWidth)));

            getLog().info(row.toString());
            printDottedLine();
        }
        return partiallyAllowedTrees;
    }

    static String center(final String text, final int width) {
        if (text == null || text.length() >= width) {
            return text.substring(0, Math.min(text.length(), width));
        }
        final var padding = width - text.length();
        final var leftPadding = padding / 2;
        final var rightPadding = padding - leftPadding;

        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }

    static String truncate(final String s, final int len) {
        if (s == null) {
            return "";
        }
        return s.length() > len ? s.substring(0, len) : s;
    }
}