package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.initializer.ClassificationRegistryInitializer;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.PackageTemplateRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.configuration.initializer.RuleRegistryInitializer;
import io.github.march_plugin.core.config.classification.model.ClassifiedComponent;
import io.github.march_plugin.core.config.classification.model.ClassifiedConcreteModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModuleReference;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.config.classification.model.PackageClassification;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.rules.RuleStrategyResolver;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluationResult;
import io.github.march_plugin.core.enforcement.dependencies.PackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.rules.RuleEnforcer;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prints a module-level dependency permission matrix across every real, fully classified module in the project.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * mvn march:module-matrix
 * }</pre>
 */
@Mojo(name = "module-matrix", aggregator = true)
public class MarchModuleMatrixMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "march.configFile", defaultValue = "${project.basedir}/march-config.xml")
    private File configFile;

    /**
     * Number of characters shown per column before a label is truncated.
     */
    @Parameter(property = "march.columnWidth", defaultValue = "4")
    private int maxColumnWidth;

    /**
     * When {@code false}, cells never show which rule matched: DEFAULT_DENY shows "OK" instead of the
     * allowing rule's letter, and DEFAULT_ALLOW shows a blank cell instead of the forbidding rule's letter.
     */
    @Parameter(property = "march.showRules", defaultValue = "true")
    private boolean showRules;

    private static final int ARTIFACT_ID_WIDTH = 30;
    private static final String OK_MARKER = "OK";

    private int fullLineWith;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.isExecutionRoot()) {
            return;
        }

        validateColumnWidth(maxColumnWidth);

        try {
            if (configFile.isFile()) {
                new MarchConfigSchemaValidator().validate(configFile);
            }
            final var marchConfigDto = new MarchConfigFileReader(configFile).readConfig();

            final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
            final var ruleRegistry = new RuleRegistryInitializer(new RuleDefinitionCompiler(dimensionRegistry)).build(marchConfigDto.rules(), marchConfigDto.ruleEngine());
            final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());
            final var packageTemplateRegistry = new PackageTemplateRegistryInitializer().build(marchConfigDto.packageTemplates());
            final var classificationRegistry = new ClassificationRegistryInitializer(projectStructureRoot, packageTemplateRegistry).build(marchConfigDto.modules().module());

            final var root = classificationRegistry.getClassifiedModule(new ModuleCoordinates(project.getGroupId(), project.getArtifactId()));
            final var allModules = new ArrayList<ClassifiedModule>();
            collectLeafModules(root, allModules);

            final var sourceModules = allModules.stream().filter(m -> m instanceof ClassifiedConcreteModule).toList();
            final var packageClassifications = classificationRegistry.getAllClassifiedPackages().stream().map(ClassifiedPackage::getClassifiedPackage).toList();

            final var ruleEnforcer = new RuleStrategyResolver(ruleRegistry.getRuleStrategy(), ruleRegistry.getScopeStrategy())
                    .getRuleEnforcer(noOpPackageDependencyEvaluator());

            printModuleLegend(dimensionRegistry, allModules, ruleRegistry.getRuleStrategy(), ruleRegistry.getScopeStrategy());
            printMatrix(ruleRegistry.getRules(), ruleEnforcer, allModules, sourceModules, packageClassifications, ruleRegistry.getRuleStrategy());
        } catch (final MarchViolationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void collectLeafModules(final ClassifiedComponent node, final List<ClassifiedModule> leaves) {
        if (node instanceof ClassifiedPackage) {
            return;
        }
        if (node instanceof ClassifiedConcreteModule concreteModule && concreteModule.getRootPackage() != null) {
            leaves.add(concreteModule);
            return;
        }
        if (node instanceof ClassifiedVirtualModuleReference virtualModuleReference) {
            leaves.add(virtualModuleReference);
            return;
        }
        for (final var child : node.getChildren()) {
            collectLeafModules(child, leaves);
        }
    }

    private static PackageDependencyEvaluator noOpPackageDependencyEvaluator() {
        return forbiddenDependency -> new PackageDependencyEvaluationResult(false, null);
    }

    private void printModuleLegend(final DimensionRegistry dimensionRegistry, final List<ClassifiedModule> allModules, final RuleStrategy ruleStrategy, final ScopeStrategy scopeStrategy) {
        getLog().info("");
        getLog().info(MessageUtils.buffer().strong("Module Dependency Matrix (real modules, cross product) — MODULE-level rules").build());
        getLog().info("Rule strategy: " + ruleStrategy + "   Scope strategy: " + scopeStrategy);
        final var fallbackPossible = ruleStrategy == RuleStrategy.DEFAULT_DENY && scopeStrategy == ScopeStrategy.AUTOMATIC;
        getLog().info(switch (ruleStrategy) {
            case DEFAULT_DENY -> showRules
                    ? "  A letter marks the rule that ALLOWS this dependency" + (fallbackPossible ? " (lowercase = only via a package-level exception, no direct module-level rule)" : "") + ". A blank cell means the dependency is FORBIDDEN by default."
                    : "  \"" + OK_MARKER + "\" means the dependency is ALLOWED by some rule (hidden). A blank cell means it is FORBIDDEN by default.";
            case DEFAULT_ALLOW -> showRules
                    ? "  A letter marks the rule that FORBIDS this dependency. \"" + OK_MARKER + "\" means the dependency is ALLOWED by default."
                    : "  A blank cell means the dependency is FORBIDDEN by some rule (hidden). \"" + OK_MARKER + "\" means it is ALLOWED by default.";
        });
        getLog().info("");
        getLog().info("Modules:");
        getLog().info("--------");

        final var sortedDimensions = dimensionRegistry.getDimensions().stream()
                .sorted(Comparator.comparing(Dimension::getName))
                .toList();
        final var maxCoordinatesLength = allModules.stream().mapToInt(m -> m.getModuleCoordinates().toString().length()).max().orElse(0);

        for (var i = 0; i < allModules.size(); i++) {
            final var module = allModules.get(i);
            final var classification = sortedDimensions.stream()
                    .map(d -> module.getClassification().getPartitions().stream().filter(p -> p.getDimension().equals(d)).findFirst())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(p -> p.getDimension().getName() + "=" + p.getName())
                    .collect(Collectors.joining(", "));

            getLog().info(String.format("%3d  %-" + maxCoordinatesLength + "s  %s", i + 1, module.getModuleCoordinates().toString(), classification));
        }
    }

    private void printMatrix(final List<Rule> rules, final RuleEnforcer ruleEnforcer, final List<ClassifiedModule> allModules, final List<ClassifiedModule> sourceModules, final List<PackageClassification> packageClassifications, final RuleStrategy ruleStrategy) {
        final var idWidth = Math.max(maxColumnWidth, String.valueOf(allModules.size()).length());
        final var rowLabelWidth = idWidth + 2 + ARTIFACT_ID_WIDTH;
        fullLineWith = rowLabelWidth + (sourceModules.size() * (idWidth + 1) + rowLabelWidth);

        getLog().info("");
        printSourceHeader(allModules, sourceModules, idWidth, rowLabelWidth, "Target \\ Source", "Source / Target");
        printDottedLine();

        final var ruleLetters = new LinkedHashMap<Rule, Character>();
        final var directlyMatchedRules = new HashSet<Rule>();

        for (var targetIndex = 0; targetIndex < allModules.size(); targetIndex++) {
            final var target = allModules.get(targetIndex);
            final var label = rowLabel(targetIndex + 1, target, idWidth);
            final var row = new StringBuilder(label);

            for (final var source : sourceModules) {
                row.append("|");
                if (source == target) {
                    row.append(center("", idWidth));
                    continue;
                }

                final var match = ruleEnforcer.matchingRulesForModuleMatrix(rules, source.getClassification(), target.getClassification(), packageClassifications);
                final var isFallbackOnly = match.directRules().isEmpty() && !match.fallbackOnlyRules().isEmpty();
                final var matchingRules = isFallbackOnly ? match.fallbackOnlyRules() : match.directRules();

                if (matchingRules.isEmpty()) {
                    final var allowedByDefault = ruleStrategy == RuleStrategy.DEFAULT_ALLOW;
                    row.append(center(allowedByDefault ? OK_MARKER : "", idWidth));
                } else if (showRules) {
                    if (!isFallbackOnly) {
                        directlyMatchedRules.addAll(matchingRules);
                    }
                    final var letters = matchingRules.stream()
                            .map(rule -> letterFor(rule, ruleLetters))
                            .sorted()
                            .map(letter -> isFallbackOnly ? Character.toLowerCase(letter) : letter)
                            .map(Object::toString)
                            .collect(Collectors.joining());
                    row.append(center(letters, idWidth));
                } else {
                    final var forbiddenByRule = ruleStrategy == RuleStrategy.DEFAULT_ALLOW;
                    row.append(center(forbiddenByRule ? "" : OK_MARKER, idWidth));
                }
            }
            row.append("| ").append(label);

            getLog().info(row.toString());
            printDottedLine();
        }

        printSourceHeader(allModules, sourceModules, idWidth, rowLabelWidth, "Target / Source", "Source \\ Target");
        printLine();

        if (showRules) {
            for (final var entry : ruleLetters.entrySet()) {
                final var letter = directlyMatchedRules.contains(entry.getKey()) ? entry.getValue() : Character.toLowerCase(entry.getValue());
                getLog().info(letter + ": " + entry.getKey().description());
            }
        } else {
            getLog().info("(rule letters hidden; pass -Dmarch.showRules=true to show which rule matched)");
        }
        printLine();
    }

    private static char letterFor(final Rule rule, final Map<Rule, Character> ruleLetters) {
        return ruleLetters.computeIfAbsent(rule, r -> (char) ('A' + ruleLetters.size()));
    }

    private static String rowLabel(final int id, final ClassifiedModule module, final int idWidth) {
        final var artifactId = module.getModuleCoordinates().getArtifactId();
        final var truncated = artifactId.length() > ARTIFACT_ID_WIDTH ? artifactId.substring(0, ARTIFACT_ID_WIDTH) : artifactId;
        return String.format("%" + idWidth + "d  %-" + ARTIFACT_ID_WIDTH + "s", id, truncated);
    }

    private void printSourceHeader(final List<ClassifiedModule> allModules, final List<ClassifiedModule> sourceModules, final int idWidth, final int rowLabelWidth, final String leftDescription, final String rightDescription) {
        final var nameParts = sourceModules.stream()
                .collect(Collectors.toMap(m -> m, m -> m.getModuleCoordinates().getArtifactId().split("-")));
        final var maxParts = nameParts.values().stream().mapToInt(parts -> parts.length).max().orElse(0);

        for (var partIndex = 0; partIndex < maxParts; partIndex++) {
            final var partIndexFinal = partIndex;
            printHeaderRow(sourceModules, idWidth, rowLabelWidth, "", "",
                    source -> {
                        final var parts = nameParts.get(source);
                        return partIndexFinal < parts.length ? parts[partIndexFinal] : "";
                    });
        }

        printHeaderRow(sourceModules, idWidth, rowLabelWidth, leftDescription, " " + rightDescription,
                source -> String.valueOf(allModules.indexOf(source) + 1));
    }

    private void printHeaderRow(final List<ClassifiedModule> sourceModules, final int idWidth, final int rowLabelWidth, final String left, final String right, final Function<ClassifiedModule, String> cellContent) {
        final var line = new StringBuilder(String.format("%-" + rowLabelWidth + "s", left));
        for (final var source : sourceModules) {
            line.append(String.format("|%-" + idWidth + "s", truncate(cellContent.apply(source), idWidth)));
        }
        line.append("|");
        line.append(String.format("%-" + rowLabelWidth + "s", right));
        getLog().info(line.toString());
    }

    static String truncate(final String s, final int len) {
        return s.length() > len ? s.substring(0, len) : s;
    }

    private void printLine() {
        getLog().info("-".repeat(fullLineWith));
    }

    private void printDottedLine() {
        getLog().info(".".repeat(fullLineWith));
    }

    static void validateColumnWidth(final int columnWidth) throws MojoExecutionException {
        if (columnWidth < 1) {
            throw new MojoExecutionException("march.columnWidth must be at least 1");
        }
    }

    static String center(final String text, final int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        final var padding = width - text.length();
        final var leftPadding = padding / 2;
        final var rightPadding = padding - leftPadding;

        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}
