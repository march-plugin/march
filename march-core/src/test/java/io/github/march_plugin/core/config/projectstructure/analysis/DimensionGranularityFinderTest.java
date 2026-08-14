package io.github.march_plugin.core.config.projectstructure.analysis;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DimensionGranularityFinderTest {

    private final DimensionGranularityFinder finder = new DimensionGranularityFinder();

    private ModuleConvention moduleConvention;
    private PackageConvention packageConvention;

    @BeforeEach
    void setUp() {
        moduleConvention = new ModuleConvention.Builder().setGroupId("io.example").setArtifactId("app").build();
        packageConvention = new PackageConvention("com.example");
    }

    private Dimension dimension(final String name, final String... partitionNames) {
        final var builder = new Dimension.Builder(name);
        for (final var partitionName : partitionNames) {
            builder.addPartition(partitionName);
        }
        return builder.build();
    }

    private DimensionPartitionGroup groupOf(final Dimension.Partition... partitions) {
        final var builder = new DimensionPartitionGroup.Builder();
        for (final var partition : partitions) {
            builder.addPartition(partition);
        }
        return builder.build();
    }

    private ModuleModularity rootModule(final Dimension dimension) {
        return new ModuleModularity.Builder(dimension, moduleConvention).buildAsRoot();
    }

    private ModuleModularity moduleChild(final ModuleModularity parent, final Dimension dimension, final DimensionPartitionGroup casePartitions) {
        final var builder = new ModuleModularity.Builder(dimension, moduleConvention);
        if (casePartitions != null) {
            builder.setCasePartitions(casePartitions);
        }
        return builder.buildAsChild(parent);
    }

    private PackageModularity packageChild(final Modularity parent, final Dimension dimension, final DimensionPartitionGroup casePartitions) {
        final var builder = new PackageModularity.Builder(dimension, packageConvention);
        if (casePartitions != null) {
            builder.setCasePartitions(casePartitions);
        }
        return builder.buildAsChild(parent);
    }

    @Test
    void shouldReturnEmptyWhenDimensionHasNoChildrenToConsumeIt() {
        final var a = dimension("a", "a1", "a2");
        final var root = rootModule(a);

        final var result = finder.findPackageOnlyDimensions(root, Set.of(a));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotReportDimensionWhoseChildrenAreModules() {
        final var a = dimension("a", "a1", "a2");
        final var root = rootModule(a);
        moduleChild(root, null, null);

        final var result = finder.findPackageOnlyDimensions(root, Set.of(a));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReportDimensionDeclaredOnAModuleTagWhoseOnlyChildrenArePackages() {
        // Mirrors the real "layer" pattern: <modularity dimension="layer"> whose children are all
        // <packageModularity>, so "layer" is never resolved during module-level classification even
        // though it is declared on a module tag.
        final var artifact = dimension("artifact", "art1", "art2");
        final var layer = dimension("layer", "service", "presentation");
        final var root = rootModule(artifact);
        final var apiModule = moduleChild(root, layer, groupOf(artifact.getPartition("art1")));
        moduleChild(root, null, groupOf(artifact.getPartition("art2")));
        packageChild(apiModule, null, groupOf(layer.getPartition("service")));
        packageChild(apiModule, null, groupOf(layer.getPartition("presentation")));

        final var result = finder.findPackageOnlyDimensions(root, Set.of(artifact, layer));

        assertThat(result).containsExactly(layer);
    }

    @Test
    void shouldReportDimensionDeclaredDeeperInsideNestedPackages() {
        final var artifact = dimension("artifact", "art1", "art2");
        final var layer = dimension("layer", "service", "presentation");
        final var abstraction = dimension("abstraction", "api", "impl");
        final var root = rootModule(artifact);
        final var apiModule = moduleChild(root, layer, null);
        final var servicePackage = packageChild(apiModule, abstraction, groupOf(layer.getPartition("service")));
        packageChild(servicePackage, null, groupOf(abstraction.getPartition("api")));
        packageChild(servicePackage, null, groupOf(abstraction.getPartition("impl")));

        final var result = finder.findPackageOnlyDimensions(root, Set.of(artifact, layer, abstraction));

        assertThat(result).containsExactlyInAnyOrder(layer, abstraction);
    }

    @Test
    void shouldNotReportDimensionThatIsConsumedByModulesInOneBranchAndPackagesInAnother() {
        final var top = dimension("top", "t1", "t2");
        final var shared = dimension("shared", "s1", "s2");
        final var other = dimension("other", "o1", "o2");
        final var root = rootModule(top);
        final var moduleBranch = moduleChild(root, shared, groupOf(top.getPartition("t1")));
        moduleChild(moduleBranch, null, null);
        final var packageBranch = moduleChild(root, other, groupOf(top.getPartition("t2")));
        final var sharedPackage = packageChild(packageBranch, shared, null);
        packageChild(sharedPackage, null, null);

        final var result = finder.findPackageOnlyDimensions(root, Set.of(shared));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldOnlyReportDimensionsThatWereRequested() {
        final var artifact = dimension("artifact", "art1", "art2");
        final var layer = dimension("layer", "service", "presentation");
        final var root = rootModule(artifact);
        final var apiModule = moduleChild(root, layer, null);
        packageChild(apiModule, null, groupOf(layer.getPartition("service")));
        packageChild(apiModule, null, groupOf(layer.getPartition("presentation")));

        final var result = finder.findPackageOnlyDimensions(root, Set.of(artifact));

        assertThat(result).isEmpty();
    }
}
