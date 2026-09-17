package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleCombinationFinderTest {

    @Test
    void aSingleUncasedChildStillLeavesTheParentsDimensionUnrepresented() {
        final var componentBuilder = new Dimension.Builder("component");
        componentBuilder.addPartition("domain");
        componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        new ModuleModularity.Builder(null, convention()).buildAsChild(root);

        final var combinations = ModuleCombinationFinder.findCombinations(root);

        final var dimensionsSeen = combinations.stream()
                .flatMap(combo -> combo.keySet().stream())
                .toList();

        assertThat(dimensionsSeen).contains(componentDim);
    }

    @Test
    void aPartitionWithNoDedicatedChildIsStillARealLeafState() {
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var apiPart = componentBuilder.addPartition("api");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(domainPart)).buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(apiPart)).buildAsChild(root);

        final var combinations = ModuleCombinationFinder.findCombinations(root);

        final var componentValuesSeen = combinations.stream()
                .map(combo -> combo.get(componentDim))
                .filter(Objects::nonNull)
                .toList();

        assertThat(componentValuesSeen).containsExactlyInAnyOrder(domainPart, apiPart, utilPart);
    }

    private static DimensionPartitionGroup caseOf(final Dimension.Partition partition) {
        return new DimensionPartitionGroup.Builder().addPartition(partition).build();
    }

    private static ModuleConvention convention() {
        return new ModuleConvention.Builder().setGroupId("com.example").setArtifactId("module").build();
    }
}
