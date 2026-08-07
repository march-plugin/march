package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.initializer.ClassificationRegistryInitializer;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.PackageTemplateRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.configuration.initializer.RuleRegistryInitializer;
import io.github.march_plugin.core.config.rules.RuleStrategyResolver;
import io.github.march_plugin.core.config.rules.parser.RuleDefinitionCompiler;
import io.github.march_plugin.core.enforcement.dependencies.ArchUnitPackageDependencyEvaluator;
import io.github.march_plugin.core.enforcement.dependencies.ModuleDependencyEnforcer;
import io.github.march_plugin.core.enforcement.project.ProjectComponentEnforcer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

@Mojo(
        name = "validate",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class MarchValidateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    // Zugriff auf alle Module im Reaktor (Multi-Module Build)
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(property = "march.configFile", defaultValue = "march-config.xml")
    private String configFile;

    @Override
    public void execute() throws MojoExecutionException {
        // Runs once per reactor build, at the LAST project Maven builds (not the execution root):
        // by then every reactor module has already been compiled, so the ArchUnit-based checks below
        // can see complete bytecode for the whole multi-module project instead of racing ahead of it.
        if (!isLastProjectInReactor()) {
            return;
        }

        final var rootProject = reactorProjects.stream()
                .filter(MavenProject::isExecutionRoot)
                .findFirst()
                .orElse(project);
        final var resolvedConfigFile = resolveConfigFile(rootProject);

        getLog().info("March Architecture Validation started using config: " + resolvedConfigFile);

        if (!resolvedConfigFile.exists()) {
            throw new MojoExecutionException("No march-config.xml found.");
        }

        // Build config
        final var marchConfigDto = new MarchConfigFileReader(resolvedConfigFile).readConfig();

        final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
        final var ruleRegistry = new RuleRegistryInitializer(new RuleDefinitionCompiler(dimensionRegistry)).build(marchConfigDto.rules());
        final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());
        final var packageTemplateRegistry = new PackageTemplateRegistryInitializer().build(marchConfigDto.packageTemplates());
        final var classificationRegistry = new ClassificationRegistryInitializer(projectStructureRoot, packageTemplateRegistry).build(marchConfigDto.modules().module());


        // Process project
        final var projectModuleRegistry = new ProjectModuleRegistryInitializer().initialize(reactorProjects);

        // Validate that configured modules exist and have configured structure
        new ProjectComponentEnforcer().validateComponentExistence(projectModuleRegistry, classificationRegistry);

        // Validate dependency definitions
        new ModuleDependencyEnforcer().validateDependencyDefinitions(projectModuleRegistry, classificationRegistry);


        final var ruleStrategyResolver = new RuleStrategyResolver(ruleRegistry.getRuleStrategy());
        final var ruleEnforcer = ruleStrategyResolver.getRuleEnforcer(new ArchUnitPackageDependencyEvaluator(projectModuleRegistry.getAllOutputDirs()));
        ruleEnforcer.enforceRules(classificationRegistry, projectModuleRegistry, ruleRegistry);

    }

    private boolean isLastProjectInReactor() {
        return !reactorProjects.isEmpty() && project.equals(reactorProjects.get(reactorProjects.size() - 1));
    }

    private File resolveConfigFile(final MavenProject rootProject) {
        final var configuredPath = new File(configFile);
        return configuredPath.isAbsolute() ? configuredPath : new File(rootProject.getBasedir(), configFile);
    }

}