package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.dto.rules.RuleSetDto;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.configuration.initializer.RuleRegistryInitializer;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.rules.config.DependencyConfig;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;
import io.github.march_plugin.core.config.rules.redundancy.RuleSetEquivalenceChecker;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks whether multiple rule sets are equivalent, meaning they allow exactly the same dependencies.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * mvn march:equivalence
 * mvn march:equivalence -Dmarch.sets=default-allow;default-deny
 * }</pre>
 */
@Mojo(name = "equivalence", aggregator = true)
public class MarchEquivalenceCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "march.configFile", defaultValue = "${project.basedir}/march-config.xml")
    private File configFile;

    /**
     * The two rule sets to compare, separated by {@code ;}.
     */
    @Parameter(property = "march.sets")
    private String sets;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.isExecutionRoot()) {
            return;
        }

        try {
            if (configFile.isFile()) {
                new MarchConfigSchemaValidator().validate(configFile);
            }
            final var marchConfigDto = new MarchConfigFileReader(configFile).readConfig();
            final var declaredRuleSets = marchConfigDto.rules();

            final var pairs = resolvePairs(sets, configFile, declaredRuleSets);

            final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
            final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());
            final var registryInitializer = new RuleRegistryInitializer(new RuleDefinitionCompiler(dimensionRegistry));

            getLog().info("");
            getLog().info(MessageUtils.buffer().strong("March Equivalence Check").build());

            var anyDisagreement = false;
            for (final var pair : pairs) {
                final var registryA = registryInitializer.build(pair.a().rules(), pair.a().config());
                final var registryB = registryInitializer.build(pair.b().rules(), pair.b().config());
                final var dependencyConfig = resolveDependencyConfig(pair.a().name(), registryA.getDependencyConfig(), pair.b().name(), registryB.getDependencyConfig());

                final var disagreement = new RuleSetEquivalenceChecker().findDisagreement(
                        registryA.getRules(), registryB.getRules(), projectStructureRoot,
                        registryA.getRuleStrategy(), registryB.getRuleStrategy(),
                        registryA.getScopeStrategy(), registryB.getScopeStrategy(), dependencyConfig);

                if (disagreement.isEmpty()) {
                    getLog().info("'%s' and '%s' are equivalent.".formatted(pair.a().name(), pair.b().name()));
                } else {
                    anyDisagreement = true;
                    report(pair.a().name(), pair.b().name(), disagreement.get());
                }
            }

            if (!anyDisagreement) {
                getLog().info("No disagreements found, over the leaf, non-reflexive classification domain.");
            }
        } catch (final MarchViolationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void report(final String nameA, final String nameB, final RuleSetEquivalenceChecker.Disagreement disagreement) {
        getLog().info("'%s' and '%s' DISAGREE (%s context, %s):".formatted(nameA, nameB, disagreement.context(), disagreement.kind()));
        getLog().info("  source: " + describe(disagreement.classification().get(PartitionExpression.Relative.Side.SOURCE)));
        getLog().info("  target: " + describe(disagreement.classification().get(PartitionExpression.Relative.Side.TARGET)));
        reportMatchingRules(nameA, disagreement.matchingRulesA());
        reportMatchingRules(nameB, disagreement.matchingRulesB());
    }

    private void reportMatchingRules(final String ruleSetName, final List<Rule> matchingRules) {
        if (matchingRules.isEmpty()) {
            return;
        }
        getLog().info("  '%s' matching rule(s):".formatted(ruleSetName));
        for (final var rule : matchingRules) {
            getLog().info("    - " + rule.description());
        }
    }

    private String describe(final Map<Dimension, Dimension.Partition> classification) {
        if (classification.isEmpty()) {
            return "(unclassified)";
        }
        return classification.entrySet().stream()
                .map(entry -> entry.getKey().getName() + "=" + entry.getValue().getName())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    static List<RuleSetPair> resolvePairs(final String sets, final File configFile, final List<RuleSetDto> declaredRuleSets) throws MojoExecutionException {
        if (sets != null) {
            final var names = sets.split(";");
            if (names.length != 2) {
                throw new MojoExecutionException("march.sets must name exactly two rule sets separated by ';' (e.g. 'default-allow;default-deny'), got '%s'.".formatted(sets));
            }
            return List.of(new RuleSetPair(findByName(declaredRuleSets, names[0], configFile), findByName(declaredRuleSets, names[1], configFile)));
        }

        if (declaredRuleSets.size() < 2) {
            throw new MojoExecutionException("march:equivalence-check needs at least two rule sets declared under <rules> in '%s' to compare; found %d.".formatted(configFile, declaredRuleSets.size()));
        }

        final var pairs = new ArrayList<RuleSetPair>();
        for (var i = 0; i < declaredRuleSets.size(); i++) {
            for (var j = i + 1; j < declaredRuleSets.size(); j++) {
                pairs.add(new RuleSetPair(declaredRuleSets.get(i), declaredRuleSets.get(j)));
            }
        }
        return pairs;
    }

    static DependencyConfig resolveDependencyConfig(final String nameA, final DependencyConfig dependencyConfigA, final String nameB, final DependencyConfig dependencyConfigB) throws MojoExecutionException {
        if (dependencyConfigA != dependencyConfigB) {
            throw new MojoExecutionException("Rule sets '%s' (%s) and '%s' (%s) declare different dependencyConfig settings; they must agree to be compared.".formatted(
                    nameA, dependencyConfigA, nameB, dependencyConfigB));
        }
        return dependencyConfigA;
    }

    static RuleSetDto findByName(final List<RuleSetDto> declaredRuleSets, final String name, final File configFile) throws MojoExecutionException {
        return declaredRuleSets.stream()
                .filter(ruleSet -> name.equals(ruleSet.name()))
                .findFirst()
                .orElseThrow(() -> new MojoExecutionException("No rule set named '%s' is declared in '%s'. Declared: %s".formatted(
                        name, configFile, declaredRuleSets.stream().map(RuleSetDto::name).collect(Collectors.joining(", ")))));
    }

    record RuleSetPair(RuleSetDto a, RuleSetDto b) {
    }
}
