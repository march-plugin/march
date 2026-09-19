package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import org.junit.jupiter.api.Test;

import java.util.Map;
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

    @Test
    void aggregateStatesAreNotLeaves() {
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(domainPart)).buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilPart)).buildAsChild(root);

        final var leaves = ModuleCombinationFinder.findLeafCombinations(root);

        assertThat(leaves).containsExactlyInAnyOrder(Map.of(componentDim, domainPart), Map.of(componentDim, utilPart));
    }

    @Test
    void aPackageWithNoFurtherCaseIsALeafDespiteHavingAnOwnDimension() {
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var layerBuilder = new Dimension.Builder("layer");
        final var businessPart = layerBuilder.addPartition("business");
        final var domainLayerPart = layerBuilder.addPartition("domain");
        final var layerDim = layerBuilder.build();

        final var packageAbstractionBuilder = new Dimension.Builder("package_abstraction");
        final var apiPart = packageAbstractionBuilder.addPartition("api");
        packageAbstractionBuilder.addPartition("impl");
        final var packageAbstractionDim = packageAbstractionBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var implModule = new ModuleModularity.Builder(layerDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        final var businessPackage = new PackageModularity.Builder(packageAbstractionDim, packageConvention())
                .setCasePartitions(caseOf(businessPart))
                .buildAsChild(implModule);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(apiPart)).buildAsChild(businessPackage);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(domainLayerPart)).buildAsChild(implModule);

        final var leaves = ModuleCombinationFinder.findLeafCombinations(root);

        final var domainLayerLeaf = leaves.stream().filter(c -> domainLayerPart.equals(c.get(layerDim))).findFirst();
        assertThat(domainLayerLeaf).isPresent();
        assertThat(domainLayerLeaf.get()).doesNotContainKey(packageAbstractionDim);
    }

    @Test
    void aModuleWithOnlyPackageChildrenIsAModuleLeaf() {
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var layerBuilder = new Dimension.Builder("layer");
        final var businessPart = layerBuilder.addPartition("business");
        final var servicePart = layerBuilder.addPartition("service");
        final var layerDim = layerBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var implModule = new ModuleModularity.Builder(layerDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(businessPart)).buildAsChild(implModule);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(servicePart)).buildAsChild(implModule);

        final var moduleLeaves = ModuleCombinationFinder.findLeafModuleCombinations(root);

        assertThat(moduleLeaves).contains(Map.of(componentDim, domainPart));
        assertThat(moduleLeaves).noneMatch(c -> c.containsKey(layerDim));
    }

    private static DimensionPartitionGroup caseOf(final Dimension.Partition partition) {
        return new DimensionPartitionGroup.Builder().addPartition(partition).build();
    }

    private static ModuleConvention convention() {
        return new ModuleConvention.Builder().setGroupId("com.example").setArtifactId("module").build();
    }

    private static PackageConvention packageConvention() {
        return new PackageConvention("com.example");
    }
}
