package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.initializer.ClassificationRegistryInitializer;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.PackageTemplateRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.core.config.classification.model.ClassifiedComponent;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModuleReference;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;

import java.io.File;

/**
 * Prints the classification tree that march builds from a configuration file, without running any rule enforcement.
 *
 * <p>{@code dimensions}, {@code projectStructure}, {@code packageTemplates} and {@code modules} are read and resolved,
 * and the resulting classification of every module and package is rendered as an indented tree.</p>
 *
 * <p>This gives a fast way to sanity-check a configuration while authoring it.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * mvn march:tree
 * }</pre>
 */
@Mojo(name = "tree", aggregator = true)
public class MarchTreeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "march.configFile", defaultValue = "${project.basedir}/march-config.xml")
    private File configFile;

    @Override
    public void execute() {
        if (!project.isExecutionRoot()) {
            return;
        }

        final var marchConfigDto = new MarchConfigFileReader(configFile).readConfig();
        final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
        final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());
        final var packageTemplateRegistry = new PackageTemplateRegistryInitializer().build(marchConfigDto.packageTemplates());
        final var classificationRegistry = new ClassificationRegistryInitializer(projectStructureRoot, packageTemplateRegistry).build(marchConfigDto.modules().module());


        getLog().info("");
        getLog().info(MessageUtils.buffer().strong("March Module Classification Tree").build());

        final var root = classificationRegistry.getClassifiedModule(new ModuleCoordinates(project.getGroupId(), project.getArtifactId()));
        renderTree(root, "", true);
    }

    private void renderTree(final ClassifiedComponent component, final String indent, final boolean isLast) {
        final var label = formatClassification(component);
        getLog().info(indent + "|--" + MessageUtils.buffer().project(component.getModuleCoordinates().getArtifactId()) + " " + label);
        final var children = component.getChildren();

        for (var i = 0; i < children.size(); i++) {
            renderTree(children.get(i), indent + (isLast ? "    " : "|   "), i == children.size() - 1);
        }
    }

    private String formatClassification(final ClassifiedComponent c) {
        if (c.getPartition() == null) {
            return "";
        }

        final var buffer = MessageUtils.buffer()
                .a("[")
                .warning(c.getPartition().getDimension().getName())
                .a("=")
                .success(c.getPartition().getName())
                .a("]");

        if (c instanceof ClassifiedVirtualModuleReference m && m.getExternalCoordinates() != null) {
            buffer.a(" ")
                    .a("(")
                    .a(m.getExternalCoordinates().getGroupId())
                    .strong(":")
                    .project(m.getExternalCoordinates().getArtifactId())
                    .a(")");
        }

        return buffer.build();
    }
}