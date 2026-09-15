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
 * Reports rules that are redundant: rules whose removal would not change the outcome for any dependency,
 * because every dependency they permit or forbid is already covered by the other configured rules.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * mvn march:redundancy
 * }</pre>
 */
@Mojo(name = "redundancy", aggregator = true)
public class MarchRedundantRulesMojo extends AbstractMojo {

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
            final var ruleRegistry = new RuleRegistryInitializer(new RuleDefinitionCompiler(dimensionRegistry)).build(marchConfigDto.rules(), marchConfigDto.ruleEngine());
            final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());

            final var redundantRules = new RuleRedundancyAnalyzer().findRedundantRules(ruleRegistry.getRules(), projectStructureRoot);

            getLog().info("");
            getLog().info(MessageUtils.buffer().strong("March Redundant Rule Analysis").build());

            if (redundantRules.isEmpty()) {
                getLog().info("No redundant rules found.");
                return;
            }

            getLog().info("The following rules never uniquely decide a match and can be removed without changing enforcement:");
            for (final var rule : redundantRules) {
                getLog().info("  - " + rule.description());
            }
        } catch (final MarchViolationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
