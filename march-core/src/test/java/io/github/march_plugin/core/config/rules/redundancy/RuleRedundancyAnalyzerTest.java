package io.github.march_plugin.core.config.rules.redundancy;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import io.github.march_plugin.core.config.rules.config.DependencyConfig;
import io.github.march_plugin.core.config.rules.config.ScopeStrategy;
import io.github.march_plugin.core.config.rules.model.Rule;
import io.github.march_plugin.core.config.rules.model.ast.ComparisonExpression;
import io.github.march_plugin.core.config.rules.model.ast.LogicalExpression;
import io.github.march_plugin.core.config.rules.model.ast.PartitionExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleRedundancyAnalyzerTest {

    private final RuleRedundancyAnalyzer analyzer = new RuleRedundancyAnalyzer();

    private static Dimension layerDim;
    private static Dimension.Partition servicePart;
    private static Dimension.Partition uiPart;

    @BeforeAll
    static void setUp() {
        final var layerBuilder = new Dimension.Builder("layer");
        servicePart = layerBuilder.addPartition("service");
        uiPart = layerBuilder.addPartition("ui");
        layerDim = layerBuilder.build();
    }

    @Test
    void flagsARuleFullySubsumedByAnIn() {
        // A: source.layer == layer.service
        final var ruleA = rule(comparisonWrap(equalTo(servicePart)));
        // B: source.layer IN layer.(service|ui)
        final var ruleB = rule(comparisonWrap(inServiceOrUi()));

        final var redundant = analyzer.findRedundantRules(List.of(ruleA, ruleB), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).containsExactly(ruleA);
    }

    @Test
    void doesNotFlagDisjointRules() {
        // A: source.layer == layer.service
        final var ruleA = rule(comparisonWrap(equalTo(servicePart)));
        // B: source.layer == layer.ui
        final var ruleB = rule(comparisonWrap(equalTo(uiPart)));

        final var redundant = analyzer.findRedundantRules(List.of(ruleA, ruleB), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).isEmpty();
    }

    @Test
    void flagsBothSidesOfAnExactDuplicate() {
        final var ruleA = rule(comparisonWrap(equalTo(servicePart)));
        final var ruleB = rule(comparisonWrap(equalTo(servicePart)));

        final var redundant = analyzer.findRedundantRules(List.of(ruleA, ruleB), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).containsExactlyInAnyOrder(ruleA, ruleB);
    }

    @Test
    void doesNotFlagASingleRuleWithNothingToCoverIt() {
        final var ruleA = rule(comparisonWrap(equalTo(servicePart)));

        final var redundant = analyzer.findRedundantRules(List.of(ruleA), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).isEmpty();
    }

    @Test
    void flagsASpecificRuleCoveredByABroaderNullCheck() {
        // A: source.layer == layer.service
        final var ruleA = rule(comparisonWrap(equalTo(servicePart)));
        // B: source.layer != NULL (true for any classified value, including "service")
        final var ruleB = rule(comparisonWrap(new ComparisonExpression.NotEqual(sourceLayer(), new PartitionExpression.Null())));

        final var redundant = analyzer.findRedundantRules(List.of(ruleA, ruleB), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).containsExactly(ruleA);
    }

    @Test
    void flagsAnAccessRuleMadeRedundantByAnAllowAllRule() {
        final var layerBuilder = new Dimension.Builder("layer");
        final var aPart = layerBuilder.addPartition("a");
        final var bPart = layerBuilder.addPartition("b");
        final var layer = layerBuilder.build();
        final var root = new ModuleModularity.Builder(layer, convention()).buildAsRoot();

        // A: source.layer == a AND target.layer == b -- "a may access b"
        final var accessRule = new Rule("a may access b", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(sourceOf(layer), new PartitionExpression.Fixed(aPart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(layer), new PartitionExpression.Fixed(bPart)))
        ), Rule.RuleScope.GLOBAL);
        // B: matches every classification unconditionally
        final var allowAllRule = new Rule("allow everything", new LogicalExpression.AlwaysTrue(), Rule.RuleScope.GLOBAL);

        final var redundant = analyzer.findRedundantRules(List.of(accessRule, allowAllRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).containsExactly(accessRule);
    }

    @Test
    void aGlobalRuleIsNotFlaggedWhileItAloneCoversTheOtherScopeContext() {
        // A (GLOBAL): source.layer == layer.service -- present in both the module and the package context
        final var globalRule = new Rule("global", comparisonWrap(equalTo(servicePart)), Rule.RuleScope.GLOBAL);
        // B (MODULE_ONLY): source.layer == layer.service -- identical condition, but only in the module context
        final var moduleOnlyDuplicate = new Rule("module-only", comparisonWrap(equalTo(servicePart)), Rule.RuleScope.MODULE_ONLY);

        final var redundant = analyzer.findRedundantRules(List.of(globalRule, moduleOnlyDuplicate), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        // moduleOnlyDuplicate contributes nothing the GLOBAL rule doesn't already cover in the module
        // context, so it is redundant. globalRule, however, is the only rule left in the package context
        // once moduleOnlyDuplicate is excluded there, so it must NOT be reported redundant.
        assertThat(redundant).containsExactly(moduleOnlyDuplicate);
    }

    @Test
    void aGlobalRuleIsFlaggedWhenCoveredInBothScopeContexts() {
        final var globalRule = new Rule("global", comparisonWrap(equalTo(servicePart)), Rule.RuleScope.GLOBAL);
        final var moduleOnlyDuplicate = new Rule("module-only", comparisonWrap(equalTo(servicePart)), Rule.RuleScope.MODULE_ONLY);
        final var packageOnlyDuplicate = new Rule("package-only", comparisonWrap(equalTo(servicePart)), Rule.RuleScope.PACKAGE_ONLY);

        final var redundant = analyzer.findRedundantRules(List.of(globalRule, moduleOnlyDuplicate, packageOnlyDuplicate), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).contains(globalRule);
    }

    @Test
    void underAutomaticAPackageOnlyRuleAlsoMakesAModuleOnlyRuleRedundant() {
        // A (MODULE_ONLY): source.layer == layer.service
        final var narrowRule = new Rule("narrow", comparisonWrap(equalTo(servicePart)), Rule.RuleScope.MODULE_ONLY);
        // B (PACKAGE_ONLY): source.layer IN layer.(service|ui) -- covers narrowRule's condition entirely
        final var broadPackageOnlyRule = new Rule("broad", comparisonWrap(inServiceOrUi()), Rule.RuleScope.PACKAGE_ONLY);

        // RuleEnforcer#matchesAtModuleLevel evaluates every rule, including PACKAGE_ONLY ones, at module level
        // when scopeStrategy is AUTOMATIC, so broadPackageOnlyRule also decides module-level dependencies here.
        final var underAutomatic = analyzer.findRedundantRules(List.of(narrowRule, broadPackageOnlyRule), null, ScopeStrategy.AUTOMATIC, DependencyConfig.ANY_LEVEL);
        assertThat(underAutomatic).containsExactly(narrowRule);

        // Under MANUAL, PACKAGE_ONLY rules never apply at module level, so nothing covers narrowRule there.
        final var underManual = analyzer.findRedundantRules(List.of(narrowRule, broadPackageOnlyRule), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(underManual).isEmpty();
    }

    @Test
    void treeRestrictionCatchesSubsumptionOnlyVisibleThroughTheProjectStructure() {
        // component/module_type mirror the real hexagonal example: module_type is only ever classified
        // under component.domain, never under component.util.
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var moduleTypeBuilder = new Dimension.Builder("module_type");
        final var modelPart = moduleTypeBuilder.addPartition("model");
        final var clientPart = moduleTypeBuilder.addPartition("client");
        final var moduleTypeDim = moduleTypeBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var domainBranch = new ModuleModularity.Builder(moduleTypeDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(modelPart)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(clientPart)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilPart)).buildAsChild(root);

        // Both MODULE_ONLY, so only the (tree-restricted) module context decides redundancy here.
        // A: target.module_type == client
        final var broaderRule = new Rule("broader", comparisonWrap(new ComparisonExpression.Equal(targetOf(moduleTypeDim), new PartitionExpression.Fixed(clientPart))), Rule.RuleScope.MODULE_ONLY);
        // B: target.module_type == client AND target.component == domain
        final var narrowerRule = new Rule("narrower", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(targetOf(moduleTypeDim), new PartitionExpression.Fixed(clientPart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(componentDim), new PartitionExpression.Fixed(domainPart)))
        ), Rule.RuleScope.MODULE_ONLY);

        // Without the tree, the dimension model alone allows component.util together with module_type.client,
        // so only the narrower rule is redundant.
        final var withoutTree = analyzer.findRedundantRules(List.of(broaderRule, narrowerRule), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(withoutTree).containsExactly(narrowerRule);

        // With the tree, module_type.client can never occur outside component.domain, so both rules become
        // interchangeable and both are reported redundant.
        final var withTree = analyzer.findRedundantRules(List.of(broaderRule, narrowerRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(withTree).containsExactlyInAnyOrder(broaderRule, narrowerRule);
    }

    @Test
    void treeRestrictionMissesSubsumptionHiddenInALeafsOwnDimension() {
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var leafLayerBuilder = new Dimension.Builder("layer");
        final var xPart = leafLayerBuilder.addPartition("x");
        leafLayerBuilder.addPartition("y");
        final var leafLayerDim = leafLayerBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();

        new ModuleModularity.Builder(leafLayerDim, convention()).setCasePartitions(caseOf(domainPart)).buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilPart)).buildAsChild(root);

        // A: target.layer == x
        final var broaderRule = new Rule("broader", comparisonWrap(new ComparisonExpression.Equal(targetOf(leafLayerDim), new PartitionExpression.Fixed(xPart))), Rule.RuleScope.MODULE_ONLY);
        // B: target.layer == x AND target.component == domain
        final var narrowerRule = new Rule("narrower", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(targetOf(leafLayerDim), new PartitionExpression.Fixed(xPart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(componentDim), new PartitionExpression.Fixed(domainPart)))
        ), Rule.RuleScope.MODULE_ONLY);

        final var withoutTree = analyzer.findRedundantRules(List.of(broaderRule, narrowerRule), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(withoutTree).containsExactly(narrowerRule);

        final var withTree = analyzer.findRedundantRules(List.of(broaderRule, narrowerRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(withTree).containsExactlyInAnyOrder(broaderRule, narrowerRule);
    }

    @Test
    void treeRestrictionAllowsClassificationsOfAggregatorModulesThemselves() {
        // Same tree as above, but this time the rule targets the "domain" aggregator module itself
        // (component.domain classified, module_type never reached) rather than one of its leaves.
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var moduleTypeBuilder = new Dimension.Builder("module_type");
        final var modelPart = moduleTypeBuilder.addPartition("model");
        final var clientPart = moduleTypeBuilder.addPartition("client");
        final var moduleTypeDim = moduleTypeBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var domainBranch = new ModuleModularity.Builder(moduleTypeDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(modelPart)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(clientPart)).buildAsChild(domainBranch);

        // A: target.component == domain AND target.module_type == NULL -- targets the "domain" aggregator
        //    module itself, which is a real classified module even though it is not a leaf.
        final var aggregatorRule = new Rule("aggregator", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(targetOf(componentDim), new PartitionExpression.Fixed(domainPart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(moduleTypeDim), new PartitionExpression.Null()))
        ), Rule.RuleScope.MODULE_ONLY);
        // B: target.module_type == client -- a disjoint leaf-level rule that does not cover A.
        final var leafRule = new Rule("leaf", comparisonWrap(new ComparisonExpression.Equal(targetOf(moduleTypeDim), new PartitionExpression.Fixed(clientPart))), Rule.RuleScope.MODULE_ONLY);

        final var redundant = analyzer.findRedundantRules(List.of(aggregatorRule, leafRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        // Neither rule is redundant: the aggregator module's own classification is a real, distinct state
        // that only the aggregator rule covers.
        assertThat(redundant).isEmpty();
    }

    @Test
    void treeRestrictionDoesNotCollapseWhenRulesSpanDisjointBranches() {
        // component/module_type/util_concern mirror the real example's component=domain vs. component=util
        // split, where the two branches use entirely different further dimensions.
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var moduleTypeBuilder = new Dimension.Builder("module_type");
        final var modelPart = moduleTypeBuilder.addPartition("model");
        moduleTypeBuilder.addPartition("client");
        final var moduleTypeDim = moduleTypeBuilder.build();

        final var utilConcernBuilder = new Dimension.Builder("util_concern");
        final var loggingPart = utilConcernBuilder.addPartition("logging");
        utilConcernBuilder.addPartition("cdi");
        final var utilConcernDim = utilConcernBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var domainBranch = new ModuleModularity.Builder(moduleTypeDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(modelPart)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(moduleTypeDim.getPartition("client"))).buildAsChild(domainBranch);
        final var utilBranch = new ModuleModularity.Builder(utilConcernDim, convention())
                .setCasePartitions(caseOf(utilPart))
                .buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(loggingPart)).buildAsChild(utilBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilConcernDim.getPartition("cdi"))).buildAsChild(utilBranch);

        // Two MODULE_ONLY rules referencing dimensions from disjoint branches; neither is covered by anything else.
        final var moduleTypeRule = new Rule("module-type", comparisonWrap(new ComparisonExpression.Equal(targetOf(moduleTypeDim), new PartitionExpression.Fixed(modelPart))), Rule.RuleScope.MODULE_ONLY);
        final var utilConcernRule = new Rule("util-concern", comparisonWrap(new ComparisonExpression.Equal(targetOf(utilConcernDim), new PartitionExpression.Fixed(loggingPart))), Rule.RuleScope.MODULE_ONLY);

        final var redundant = analyzer.findRedundantRules(List.of(moduleTypeRule, utilConcernRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(redundant).isEmpty();
    }

    @Test
    void treeRestrictionAppliesAtThePackageLevelToo() {
        // component/layer_kind mirror the module-level tree restriction test, but layer_kind is classified
        // by package modularities nested under the "domain" module rather than by further module modularities:
        // layer_kind.impl is only ever reachable under component.domain, never under component.util.
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var layerKindBuilder = new Dimension.Builder("layer_kind");
        final var apiPart = layerKindBuilder.addPartition("api");
        final var implPart = layerKindBuilder.addPartition("impl");
        final var layerKindDim = layerKindBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var domainModule = new ModuleModularity.Builder(layerKindDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(apiPart)).buildAsChild(domainModule);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(implPart)).buildAsChild(domainModule);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilPart)).buildAsChild(root);

        // Both PACKAGE_ONLY, so only the (tree-restricted) package context decides redundancy here.
        // A: target.layer_kind == impl
        final var broaderRule = new Rule("broader", comparisonWrap(new ComparisonExpression.Equal(targetOf(layerKindDim), new PartitionExpression.Fixed(implPart))), Rule.RuleScope.PACKAGE_ONLY);
        // B: target.layer_kind == impl AND target.component == domain
        final var narrowerRule = new Rule("narrower", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(targetOf(layerKindDim), new PartitionExpression.Fixed(implPart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(componentDim), new PartitionExpression.Fixed(domainPart)))
        ), Rule.RuleScope.PACKAGE_ONLY);

        // Without the tree, the dimension model alone allows component.util together with layer_kind.impl,
        // so only the narrower rule is redundant.
        final var withoutTree = analyzer.findRedundantRules(List.of(broaderRule, narrowerRule), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(withoutTree).containsExactly(narrowerRule);

        // With the tree, layer_kind.impl can never occur outside component.domain, so both rules become
        // interchangeable and both are reported redundant.
        final var withTree = analyzer.findRedundantRules(List.of(broaderRule, narrowerRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(withTree).containsExactlyInAnyOrder(broaderRule, narrowerRule);
    }

    @Test
    void moduleContextIgnoresCombinationsOnlyReachableThroughAPackage() {
        // Same tree as treeRestrictionAppliesAtThePackageLevelToo: layer_kind is classified by package
        // modularities nested under the "domain" module. No real module is ever classified by layer_kind,
        // only packages beneath domainModule are.
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var layerKindBuilder = new Dimension.Builder("layer_kind");
        final var apiPart = layerKindBuilder.addPartition("api");
        layerKindBuilder.addPartition("impl");
        final var layerKindDim = layerKindBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var domainModule = new ModuleModularity.Builder(layerKindDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new PackageModularity.Builder(null, packageConvention()).setCasePartitions(caseOf(apiPart)).buildAsChild(domainModule);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilPart)).buildAsChild(root);

        // MODULE_ONLY, so only the module context decides reachability here.
        final var moduleOnlyRule = new Rule("module-only", comparisonWrap(new ComparisonExpression.Equal(targetOf(layerKindDim), new PartitionExpression.Fixed(apiPart))), Rule.RuleScope.MODULE_ONLY);

        final var unreachable = analyzer.findUnreachableRules(List.of(moduleOnlyRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        // No real module is ever classified by layer_kind, only packages under domainModule are, so this
        // MODULE_ONLY rule can never actually fire on a real module dependency and must be unreachable.
        assertThat(unreachable).containsExactly(moduleOnlyRule);
    }

    @Test
    void flagsAContradictoryRuleAsUnreachableEvenWithoutATree() {
        // target.layer == service AND target.layer == ui can never both hold: a component has exactly
        // one classification per dimension.
        final var contradictoryRule = new Rule("contradictory", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(targetOf(layerDim), new PartitionExpression.Fixed(servicePart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(layerDim), new PartitionExpression.Fixed(uiPart)))
        ), Rule.RuleScope.GLOBAL);

        final var unreachable = analyzer.findUnreachableRules(List.of(contradictoryRule), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(unreachable).containsExactly(contradictoryRule);
    }

    @Test
    void doesNotFlagAReachableRuleAsUnreachable() {
        final var ruleA = rule(comparisonWrap(equalTo(servicePart)));
        final var ruleB = rule(comparisonWrap(equalTo(uiPart)));

        final var unreachable = analyzer.findUnreachableRules(List.of(ruleA, ruleB), null, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);

        assertThat(unreachable).isEmpty();
    }

    @Test
    void flagsAnUnreachableRuleInsteadOfRedundant() {
        // component/module_type mirror the earlier tree restriction tests: module_type is only ever
        // classified under component.domain, never under component.util.
        final var componentBuilder = new Dimension.Builder("component");
        final var domainPart = componentBuilder.addPartition("domain");
        final var utilPart = componentBuilder.addPartition("util");
        final var componentDim = componentBuilder.build();

        final var moduleTypeBuilder = new Dimension.Builder("module_type");
        final var modelPart = moduleTypeBuilder.addPartition("model");
        moduleTypeBuilder.addPartition("client");
        final var moduleTypeDim = moduleTypeBuilder.build();

        final var root = new ModuleModularity.Builder(componentDim, convention()).buildAsRoot();
        final var domainBranch = new ModuleModularity.Builder(moduleTypeDim, convention())
                .setCasePartitions(caseOf(domainPart))
                .buildAsChild(root);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(modelPart)).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(moduleTypeDim.getPartition("client"))).buildAsChild(domainBranch);
        new ModuleModularity.Builder(null, convention()).setCasePartitions(caseOf(utilPart)).buildAsChild(root);

        // Impossible: module_type.model can never occur together with component.util, likely a copy-paste
        // mistake rather than an intentionally strict rule.
        final var impossibleRule = new Rule("impossible", new LogicalExpression.And(
                comparisonWrap(new ComparisonExpression.Equal(targetOf(moduleTypeDim), new PartitionExpression.Fixed(modelPart))),
                comparisonWrap(new ComparisonExpression.Equal(targetOf(componentDim), new PartitionExpression.Fixed(utilPart)))
        ), Rule.RuleScope.MODULE_ONLY);

        final var unreachable = analyzer.findUnreachableRules(List.of(impossibleRule), root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(unreachable).containsExactly(impossibleRule);

        // A caller that excludes rules already found unreachable, as findRedundantRules's contract requires,
        // never sees this rule reported as redundant, even though its own literal is trivially unsatisfiable
        // and would otherwise make every assumptionsFor() query against it trivially unsatisfiable too.
        final var rulesToCheck = List.of(impossibleRule).stream().filter(rule -> !unreachable.contains(rule)).toList();
        final var redundant = analyzer.findRedundantRules(rulesToCheck, root, ScopeStrategy.MANUAL, DependencyConfig.ANY_LEVEL);
        assertThat(redundant).isEmpty();
    }

    private static ModuleConvention convention() {
        return new ModuleConvention.Builder().setGroupId("com.example").setArtifactId("module").build();
    }

    private static DimensionPartitionGroup caseOf(final Dimension.Partition partition) {
        return new DimensionPartitionGroup.Builder().addPartition(partition).build();
    }

    private static PackageConvention packageConvention() {
        return new PackageConvention("com.example");
    }

    private static PartitionExpression.Relative targetOf(final Dimension dimension) {
        return new PartitionExpression.Relative(PartitionExpression.Relative.Side.TARGET, dimension);
    }

    private static PartitionExpression.Relative sourceOf(final Dimension dimension) {
        return new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, dimension);
    }

    private static Rule rule(final LogicalExpression definition) {
        return new Rule("test rule", definition, Rule.RuleScope.GLOBAL);
    }

    private static LogicalExpression.ComparisonWrap comparisonWrap(final ComparisonExpression comparison) {
        return new LogicalExpression.ComparisonWrap(comparison);
    }

    private static PartitionExpression.Relative sourceLayer() {
        return new PartitionExpression.Relative(PartitionExpression.Relative.Side.SOURCE, layerDim);
    }

    private static ComparisonExpression.Equal equalTo(final Dimension.Partition partition) {
        return new ComparisonExpression.Equal(sourceLayer(), new PartitionExpression.Fixed(partition));
    }

    private static ComparisonExpression.In inServiceOrUi() {
        return new ComparisonExpression.In(sourceLayer(), List.of(new PartitionExpression.Fixed(servicePart), new PartitionExpression.Fixed(uiPart)));
    }
}
