package io.github.march_plugin.core.config.rules.evaluation;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.rules.config.RuleRegistry;
import io.github.march_plugin.core.config.rules.config.RuleStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDenyDependencyPermissionEvaluatorTest {

    private final Dimension.Builder domainBuilder = new Dimension.Builder("domain");
    private final Dimension.Partition domainA = domainBuilder.addPartition("a");
    private final Dimension.Partition domainB = domainBuilder.addPartition("b");
    private final Dimension domain = domainBuilder.build();

    private static Set<Dimension.Partition> mutable(final Dimension.Partition... partitions) {
        return new HashSet<>(Set.of(partitions));
    }

    private final LogicalExpression sameDomainRule = new LogicalExpression.ComparisonWrap(
            new ComparisonExpression.Equal(
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, domain),
                    new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, domain)));

    private final Dimension.Builder rootBuilder = new Dimension.Builder("scope");
    private final Dimension.Partition rootX = rootBuilder.addPartition("x");
    private final Dimension.Partition rootY = rootBuilder.addPartition("y");
    private final Dimension rootDimension = rootBuilder.build();

    private final DimensionRegistry dimensionRegistry = new DimensionRegistry.Builder().addDimension(domain).build();

    private final ModuleModularity projectStructureRoot = new ModuleModularity.Builder(
            rootDimension, new ModuleConvention.Builder().setGroupId("io.example").setArtifactId("app").build())
            .buildAsRoot();

    private final DefaultDenyDependencyPermissionEvaluator evaluator = new DefaultDenyDependencyPermissionEvaluator(projectStructureRoot);

    private RuleRegistry registryOf(final Rule... rules) {
        final var builder = new RuleRegistry.Builder();
        builder.setRuleStrategy(RuleStrategy.DEFAULT_DENY);
        for (final var rule : rules) {
            builder.addRule(rule);
        }
        return builder.build();
    }

    @Test
    void allowsWhenARuleReducesToAlwaysTrue() {
        final var rule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.GLOBAL);
        final var registry = registryOf(rule);

        final var result = evaluator.reduce(registry, dimensionRegistry, mutable(domainA), mutable(domainA));

        assertThat(result).isInstanceOf(DependencyPermission.Allowed.class);
    }

    @Test
    void forbidsWhenNoRuleMatches() {
        final var rule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.GLOBAL);
        final var registry = registryOf(rule);

        final var result = evaluator.reduce(registry, dimensionRegistry, mutable(domainA), mutable(domainB));

        assertThat(result).isInstanceOf(DependencyPermission.Forbidden.class);
    }

    @Test
    void ignoresModuleOnlyRules() {
        final var moduleOnlyRule = new Rule("Same domain", sameDomainRule, Rule.RuleScope.MODULE_ONLY);
        final var registry = registryOf(moduleOnlyRule);

        final var result = evaluator.reduce(registry, dimensionRegistry, mutable(domainA), mutable(domainA));

        assertThat(result).isInstanceOf(DependencyPermission.Forbidden.class);
    }
}
