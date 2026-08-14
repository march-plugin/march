package io.github.march_plugin.core.config.projectstructure.analysis;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PossibleCombinationsFinderTest {

    private final PossibleCombinationsFinder finder = new PossibleCombinationsFinder();

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

    @Nested
    class SingleDimension {

        @Test
        void shouldReturnOnePartitionSetPerPartitionAtRoot() {
            final var a = dimension("a", "a1", "a2");
            final var root = rootModule(a);

            final var combinations = finder.findCombinations(root, Set.of(a));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1")),
                    Set.of(a.getPartition("a2")));
        }

        @Test
        void shouldNotDescendIntoChildrenWhenOnlyRootDimensionRequested() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var root = rootModule(a);
            moduleChild(root, b, null);

            final var combinations = finder.findCombinations(root, Set.of(a));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1")),
                    Set.of(a.getPartition("a2")));
        }
    }

    @Nested
    class MultipleDimensionsAcrossModuleLevels {

        @Test
        void shouldReturnCrossProductForNestedUncasedModules() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var root = rootModule(a);
            moduleChild(root, b, null);

            final var combinations = finder.findCombinations(root, Set.of(a, b));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1"), b.getPartition("b1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2")),
                    Set.of(a.getPartition("a2"), b.getPartition("b1")),
                    Set.of(a.getPartition("a2"), b.getPartition("b2")));
        }
    }

    @Nested
    class CaseBasedBranching {

        @Test
        void shouldOnlyCombinePartitionsReachableOnTheSameBranch() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var c = dimension("c", "c1", "c2");
            final var root = rootModule(a);
            moduleChild(root, b, groupOf(a.getPartition("a1")));
            moduleChild(root, c, groupOf(a.getPartition("a2")));

            final var combinations = finder.findCombinations(root, Set.of(a, b));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1"), b.getPartition("b1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2")));
        }

        @Test
        void shouldOnlyCombinePartitionsReachableOnTheOtherBranch() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var c = dimension("c", "c1", "c2");
            final var root = rootModule(a);
            moduleChild(root, b, groupOf(a.getPartition("a1")));
            moduleChild(root, c, groupOf(a.getPartition("a2")));

            final var combinations = finder.findCombinations(root, Set.of(a, c));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a2"), c.getPartition("c1")),
                    Set.of(a.getPartition("a2"), c.getPartition("c2")));
        }

        @Test
        void shouldReturnEmptyWhenRequestedDimensionsNeverCoOccurOnAnyBranch() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var c = dimension("c", "c1", "c2");
            final var root = rootModule(a);
            moduleChild(root, b, groupOf(a.getPartition("a1")));
            moduleChild(root, c, groupOf(a.getPartition("a2")));

            final var combinations = finder.findCombinations(root, Set.of(b, c));

            assertThat(combinations).isEmpty();
        }
    }

    @Nested
    class PackageModularityTraversal {

        @Test
        void shouldTraverseIntoSolePackageModularityChild() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var root = rootModule(a);
            packageChild(root, b, null);

            final var combinations = finder.findCombinations(root, Set.of(a, b));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1"), b.getPartition("b1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2")),
                    Set.of(a.getPartition("a2"), b.getPartition("b1")),
                    Set.of(a.getPartition("a2"), b.getPartition("b2")));
        }

        @Test
        void shouldTraverseIntoNestedPackageModularityChildren() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var c = dimension("c", "c1", "c2");
            final var root = rootModule(a);
            final var pkg = packageChild(root, b, null);
            packageChild(pkg, c, null);

            final var combinations = finder.findCombinations(root, Set.of(a, b, c));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1"), b.getPartition("b1"), c.getPartition("c1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b1"), c.getPartition("c2")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2"), c.getPartition("c1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2"), c.getPartition("c2")),
                    Set.of(a.getPartition("a2"), b.getPartition("b1"), c.getPartition("c1")),
                    Set.of(a.getPartition("a2"), b.getPartition("b1"), c.getPartition("c2")),
                    Set.of(a.getPartition("a2"), b.getPartition("b2"), c.getPartition("c1")),
                    Set.of(a.getPartition("a2"), b.getPartition("b2"), c.getPartition("c2")));
        }

        @Test
        void shouldFindPackageOnlyDimensionEvenWhenItsAncestorDimensionIsNotRequested() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var root = rootModule(a);
            packageChild(root, b, null);

            final var combinations = finder.findCombinations(root, Set.of(b));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(b.getPartition("b1")),
                    Set.of(b.getPartition("b2")));
        }
    }

    @Nested
    class MixedModuleAndPackageDimensions {

        @Test
        void shouldCombineModuleLevelAndPackageLevelDimensions() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var c = dimension("c", "c1", "c2");
            final var root = rootModule(a);
            final var module = moduleChild(root, b, null);
            packageChild(module, c, null);

            final var combinations = finder.findCombinations(root, Set.of(a, b, c));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(a.getPartition("a1"), b.getPartition("b1"), c.getPartition("c1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b1"), c.getPartition("c2")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2"), c.getPartition("c1")),
                    Set.of(a.getPartition("a1"), b.getPartition("b2"), c.getPartition("c2")),
                    Set.of(a.getPartition("a2"), b.getPartition("b1"), c.getPartition("c1")),
                    Set.of(a.getPartition("a2"), b.getPartition("b1"), c.getPartition("c2")),
                    Set.of(a.getPartition("a2"), b.getPartition("b2"), c.getPartition("c1")),
                    Set.of(a.getPartition("a2"), b.getPartition("b2"), c.getPartition("c2")));
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void shouldReturnEmptyWhenRequestedDimensionDoesNotExistInTree() {
            final var a = dimension("a", "a1", "a2");
            final var unrelated = dimension("unrelated", "x1", "x2");
            final var root = rootModule(a);

            final var combinations = finder.findCombinations(root, Set.of(unrelated));

            assertThat(combinations).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoDimensionsRequested() {
            final var a = dimension("a", "a1", "a2");
            final var root = rootModule(a);

            final var combinations = finder.findCombinations(root, Set.of());

            assertThat(combinations).isEmpty();
        }

        @Test
        void shouldDeduplicateIdenticalCombinationsReachedViaDifferentBranches() {
            final var a = dimension("a", "a1", "a2");
            final var b = dimension("b", "b1", "b2");
            final var root = rootModule(a);
            moduleChild(root, b, groupOf(a.getPartition("a1")));
            moduleChild(root, b, groupOf(a.getPartition("a2")));

            final var combinations = finder.findCombinations(root, Set.of(b));

            assertThat(combinations).containsExactlyInAnyOrder(
                    Set.of(b.getPartition("b1")),
                    Set.of(b.getPartition("b2")));
        }
    }
}
