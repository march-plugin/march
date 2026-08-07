package io.github.march_plugin;

import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.project.ProjectModuleRegistry;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a {@link ProjectModuleRegistry} from the Maven projects in the reactor.
 */
public class ProjectModuleRegistryInitializer {

    /**
     * Builds the project module registry from the given Maven projects.
     *
     * @param mavenProjects the Maven projects in the reactor
     * @return the project module registry populated with the reactor's modules
     */
    public ProjectModuleRegistry initialize(final List<MavenProject> mavenProjects) {
        return new ProjectModuleRegistry(
                mavenProjects.stream().collect(Collectors.toMap(
                        subProject -> new ModuleCoordinates(subProject.getGroupId(), subProject.getArtifactId()),
                        subProject -> new ProjectModuleRegistry.ProjectModuleInfo(
                                subProject.getBasedir().toPath(),
                                getOutputFiles(subProject),
                                mapToRawDependencies(subProject.getOriginalModel().getDependencies()),
                                subProject.getOriginalModel().getDependencyManagement() != null
                                        ? mapToRawDependencies(subProject.getOriginalModel().getDependencyManagement().getDependencies())
                                        : List.of()
                        )
                )));
    }

    private List<ProjectModuleRegistry.ProjectModuleInfo.RawDependency> mapToRawDependencies(final List<Dependency> mavenDependencies) {

        return mavenDependencies.stream()
                .map(dep -> new ProjectModuleRegistry.ProjectModuleInfo.RawDependency(
                        new ModuleCoordinates(dep.getGroupId(), dep.getArtifactId()),
                        dep.getVersion(),
                        dep.getScope(),
                        dep.getExclusions().stream()
                                .map(ex -> new ModuleCoordinates(ex.getGroupId(), ex.getArtifactId()))
                                .collect(Collectors.toSet())
                ))
                .toList();
    }


    private Path getOutputFiles(final MavenProject mavenProject) {
        final var file = new File(mavenProject.getBuild().getOutputDirectory());

        if (file.exists()) {
            return file.toPath();
        }
        return null;
    }

}
