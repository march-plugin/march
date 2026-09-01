package io.github.march_plugin;

import io.github.march_plugin.configuration.deserializer.MarchConfigFileReader;
import io.github.march_plugin.configuration.initializer.ClassificationRegistryInitializer;
import io.github.march_plugin.configuration.initializer.DimensionRegistryInitializer;
import io.github.march_plugin.configuration.initializer.PackageTemplateRegistryInitializer;
import io.github.march_plugin.configuration.initializer.ProjectStructureInitializer;
import io.github.march_plugin.core.config.classification.model.ClassifiedComponent;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModuleReference;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.exceptions.MarchViolationException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
 * mvn march:tree -Dmarch.showInherited=true
 * }</pre>
 */
@Mojo(name = "tree", aggregator = true)
public class MarchTreeMojo extends AbstractMojo {

    private static final String MODULE_COLOR = "[33m"; // yellow
    private static final String OWN_DIMENSION_COLOR = "[35m"; // purple
    private static final String OWN_PARTITION_VALUE_COLOR = "[94m"; // light blue
    private static final String INHERITED_DIMENSION_COLOR = "[38;5;248m"; // gray (slightly lighter mid-tone, readable on both light and dark backgrounds)
    private static final String INHERITED_PARTITION_COLOR = "[96m"; // light cyan
    private static final String ANSI_RESET = "[0m";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "march.configFile", defaultValue = "${project.basedir}/march-config.xml")
    private File configFile;

    /**
     * Whether each node also shows inherited classifications.
     */
    @Parameter(property = "march.showInherited", defaultValue = "false")
    private boolean showInherited;

    @Override
    public void execute() throws MojoFailureException {
        if (!project.isExecutionRoot()) {
            return;
        }

        try {
            final var marchConfigDto = new MarchConfigFileReader(configFile).readConfig();
            final var dimensionRegistry = new DimensionRegistryInitializer().build(marchConfigDto.dimensions());
            final var projectStructureRoot = new ProjectStructureInitializer(dimensionRegistry).build(marchConfigDto.projectStructure());
            final var packageTemplateRegistry = new PackageTemplateRegistryInitializer().build(marchConfigDto.packageTemplates());
            final var classificationRegistry = new ClassificationRegistryInitializer(projectStructureRoot, packageTemplateRegistry).build(marchConfigDto.modules().module());


            getLog().info("");
            getLog().info(MessageUtils.buffer().strong("March Module Classification Tree").build());

            final var root = classificationRegistry.getClassifiedModule(new ModuleCoordinates(project.getGroupId(), project.getArtifactId()));
            renderTree(root, "", true, List.of());
        } catch (final MarchViolationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void renderTree(final ClassifiedComponent component, final String indent, final boolean isLast, final List<Dimension.Partition> ancestorPath) {
        final var label = formatClassification(component, ancestorPath);
        getLog().info(indent + "|--" + colorize(component.getModuleCoordinates().getArtifactId(), MODULE_COLOR) + " " + label);

        final var ownPath = component.getPartition() == null ? ancestorPath : append(ancestorPath, component.getPartition());
        final var children = component.getChildren();

        for (var i = 0; i < children.size(); i++) {
            renderTree(children.get(i), indent + (isLast ? "    " : "|   "), i == children.size() - 1, ownPath);
        }
    }

    private static List<Dimension.Partition> append(final List<Dimension.Partition> path, final Dimension.Partition partition) {
        final var extended = new ArrayList<>(path);
        extended.add(partition);
        return List.copyOf(extended);
    }

    private String formatClassification(final ClassifiedComponent c, final List<Dimension.Partition> ancestorPath) {
        final var partitions = showInherited
                ? (c.getPartition() == null ? ancestorPath : append(ancestorPath, c.getPartition()))
                : (c.getPartition() == null ? List.<Dimension.Partition>of() : List.of(c.getPartition()));

        if (partitions.isEmpty()) {
            return "";
        }

        final var buffer = MessageUtils.buffer().a("[");
        for (var i = 0; i < partitions.size(); i++) {
            if (i > 0) {
                buffer.a("; ");
            }
            final var partition = partitions.get(i);
            if (partition.equals(c.getPartition())) {
                buffer.a(colorize(partition.getDimension().getName(), OWN_DIMENSION_COLOR)).a("=").a(colorize(partition.getName(), OWN_PARTITION_VALUE_COLOR));
            } else {
                buffer.a(colorize(partition.getDimension().getName(), INHERITED_DIMENSION_COLOR))
                        .a("=")
                        .a(colorize(partition.getName(), INHERITED_PARTITION_COLOR));
            }
        }
        buffer.a("]");

        if (c instanceof ClassifiedVirtualModuleReference m && m.getExternalCoordinates() != null) {
            buffer.a(" ")
                    .a("(")
                    .a(m.getExternalCoordinates().getGroupId())
                    .strong(":")
                    .a(colorize(m.getExternalCoordinates().getArtifactId(), MODULE_COLOR))
                    .a(")");
        }

        return buffer.build();
    }

    private static String colorize(final String text, final String ansiColor) {
        return MessageUtils.isColorEnabled() ? ansiColor + text + ANSI_RESET : text;
    }
}