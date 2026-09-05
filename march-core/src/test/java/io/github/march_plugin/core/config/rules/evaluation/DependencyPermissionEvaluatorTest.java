package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A dimension not part of a matrix query can still be structurally forced to a single value by the
 * modularity tree (e.g. "component" is always "domain" for any order/article package, never "util"), even
 * though it isn't explicitly among the requested dimensions. {@link DependencyPermissionEvaluator#getForcedDimensionValues}
 * is what detects this, letting {@link RuleReducer} treat such a dimension as resolved instead of unknown.
 */
class DependencyPermissionEvaluatorTest {

    private final Dimension.Builder componentBuilder = new Dimension.Builder("component");
    private final Dimension.Partition componentDomain = componentBuilder.addPartition("domain");
    private final Dimension.Partition componentUtil = componentBuilder.addPartition("util");
    private final Dimension component = componentBuilder.build();

    private final Dimension.Builder domainBuilder = new Dimension.Builder("domain");
    private final Dimension.Partition domainOrder = domainBuilder.addPartition("order");
    private final Dimension.Partition domainArticle = domainBuilder.addPartition("article");
    private final Dimension domain = domainBuilder.build();

    private final Dimension.Builder layerBuilder = new Dimension.Builder("layer");
    private final Dimension.Partition layerPresentation = layerBuilder.addPartition("presentation");
    private final Dimension.Partition layerService = layerBuilder.addPartition("service");
    private final Dimension layer = layerBuilder.build();

    private final Dimension.Builder unusedBuilder = new Dimension.Builder("unused");
    private final Dimension.Partition unusedA = unusedBuilder.addPartition("a");
    private final Dimension.Partition unusedB = unusedBuilder.addPartition("b");
    private final Dimension unused = unusedBuilder.build();

    private final DimensionRegistry dimensionRegistry = new DimensionRegistry.Builder()
            .addDimension(component).addDimension(domain).addDimension(layer).addDimension(unused).build();

    private final ModuleConvention convention = new ModuleConvention.Builder().setGroupId("io.example").setArtifactId("app").build();

    private static DimensionPartitionGroup groupOf(final Dimension.Partition... partitions) {
        final var builder = new DimensionPartitionGroup.Builder();
        for (final var partition : partitions) {
            builder.addPartition(partition);
        }
        return builder.build();
    }

    /**
     * Mirrors march-example-4-layer's tree, minus the artifact/util-branch noise.
     */
    private ModuleModularity buildTree() {
        final var root = new ModuleModularity.Builder(component, convention).buildAsRoot();

        final var domainBranch = new ModuleModularity.Builder(domain, convention).setCasePartitions(groupOf(componentDomain)).buildAsChild(root);
        new ModuleModularity.Builder(null, convention).setCasePartitions(groupOf(componentUtil)).buildAsChild(root);

        final var order = new ModuleModularity.Builder(layer, convention).setCasePartitions(groupOf(domainOrder)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention).setCasePartitions(groupOf(layerPresentation)).buildAsChild(order);
        new ModuleModularity.Builder(null, convention).setCasePartitions(groupOf(layerService)).buildAsChild(order);

        final var article = new ModuleModularity.Builder(layer, convention).setCasePartitions(groupOf(domainArticle)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention).setCasePartitions(groupOf(layerPresentation)).buildAsChild(article);
        new ModuleModularity.Builder(null, convention).setCasePartitions(groupOf(layerService)).buildAsChild(article);

        return root;
    }

    private static class TestableEvaluator extends DependencyPermissionEvaluator {
        TestableEvaluator(final ModuleModularity projectStructureRoot) {
            super(projectStructureRoot);
        }

        @Override
        public DependencyPermission reduce(final RuleRegistry ruleRegistry, final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> source, final Set<Dimension.Partition> target) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        Map<Dimension, Dimension.Partition> forcedValues(final DimensionRegistry dimensionRegistry, final Set<Dimension.Partition> partitions) {
            return getForcedDimensionValues(dimensionRegistry, partitions);
        }
    }

    @Test
    void componentIsForcedToDomainForAnyOrderOrArticlePartition() {
        final var evaluator = new TestableEvaluator(buildTree());

        final var forced = evaluator.forcedValues(dimensionRegistry, Set.of(domainOrder, layerPresentation));

        assertThat(forced).containsEntry(component, componentDomain);
    }

    @Test
    void layerStaysAmbiguousWhenNotYetNarrowedDown() {
        final var evaluator = new TestableEvaluator(buildTree());

        final var forced = evaluator.forcedValues(dimensionRegistry, Set.of(domainOrder));

        assertThat(forced).containsEntry(component, componentDomain);
        assertThat(forced).doesNotContainKey(layer);
    }

    @Test
    void dimensionNeverUsedInTheTreeIsForcedToAlwaysAbsent() {
        final var evaluator = new TestableEvaluator(buildTree());

        final var forced = evaluator.forcedValues(dimensionRegistry, Set.of(domainOrder, layerPresentation));

        assertThat(forced).containsKey(unused);
        assertThat(forced.get(unused)).isNull();
    }

    @Test
    void groupCasesKeepTheDimensionAmbiguousRatherThanForcingAWrongValue() {
        final var root = new ModuleModularity.Builder(component, convention).buildAsRoot();
        final var domainBranch = new ModuleModularity.Builder(domain, convention).setCasePartitions(groupOf(componentDomain)).buildAsChild(root);
        new ModuleModularity.Builder(null, convention).setCasePartitions(groupOf(componentUtil)).buildAsChild(root);
        // Both order and article share one node here instead of being split apart: domain resolves to a
        // group of two partitions at this point, not to a single one.
        new ModuleModularity.Builder(layer, convention).setCasePartitions(groupOf(domainOrder, domainArticle)).buildAsChild(domainBranch);

        final var evaluator = new TestableEvaluator(root);
        final var forced = evaluator.forcedValues(dimensionRegistry, Set.of(layerPresentation));

        assertThat(forced).containsEntry(component, componentDomain);
        assertThat(forced).doesNotContainKey(domain);
    }
}
