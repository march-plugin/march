package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.configuration.initializer.RuleRegistryInitializer;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;
import io.github.march_plugin.core.config.rules.redundancy.RuleRedundancyAnalyzer;
import io.github.march_plugin.core.exceptions.MarchViolationException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;

import java.io.File;

/**
 * Checks the configured rules for issues that a schema validation cannot catch:
 * - rules that can never match any real classification
 * - rules whose removal would not change the outcome for any dependency.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * mvn march:config-check
 * }</pre>
 */
@Mojo(name = "config-check", aggregator = true)
public class MarchConfigCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "march.configFile", defaultValue = "${project.basedir}/march-config.xml")
    private File configFile;

    @Override
    public void execute() throws MojoFailureException {
        if (!project.isExecutionRoot()) {
            return;
        }

        try {
            if (configFile.isFile()) {
                new MarchConfigSchemaValidator().validate(configFile);
            }
            final var marchConfigDto = new MarchConfigFileReader(configFile).readConfig();
            final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
            final var ruleRegistry = new RuleRegistryInitializer(new RuleDefinitionCompiler(dimensionRegistry)).buildActive(marchConfigDto);
            final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());

            final var analyzer = new RuleRedundancyAnalyzer();
            final var allRules = ruleRegistry.getRules();
            final var scopeStrategy = ruleRegistry.getScopeStrategy();
            final var dependencyConfig = ruleRegistry.getDependencyConfig();
            final var unreachableRules = analyzer.findUnreachableRules(allRules, projectStructureRoot, scopeStrategy, dependencyConfig);
            final var rulesToCheck = allRules.stream().filter(rule -> !unreachableRules.contains(rule)).toList();
            final var redundantRules = analyzer.findRedundantRules(rulesToCheck, projectStructureRoot, scopeStrategy, dependencyConfig);

            getLog().info("");
            getLog().info(MessageUtils.buffer().strong("March Config Check").build());

            if (unreachableRules.isEmpty() && redundantRules.isEmpty()) {
                getLog().info("No redundant or unreachable rules found.");
                return;
            }

            if (!unreachableRules.isEmpty()) {
                getLog().info("The following rules can never match any real classification and should be corrected or removed:");
                for (final var rule : unreachableRules) {
                    getLog().info("  - " + rule.description());
                }
            }

            if (!redundantRules.isEmpty()) {
                getLog().info("The following rules never uniquely decide a match and can be removed without changing enforcement:");
                for (final var rule : redundantRules) {
                    getLog().info("  - " + rule.description());
                }
            }
        } catch (final MarchViolationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
