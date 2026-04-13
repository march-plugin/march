package io.github.march_plugin.core.config.projectstructure;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.projectstructure.exception.DifferentChildModularityTypeException;
import io.github.march_plugin.core.config.projectstructure.exception.DuplicateCasePartitionException;
import io.github.march_plugin.core.config.projectstructure.exception.DuplicateDimensionInPathException;
import io.github.march_plugin.core.config.projectstructure.exception.EmptyModularityDimensionException;
import io.github.march_plugin.core.config.projectstructure.exception.NoCaseDefinedForMultipleChildrenException;
import io.github.march_plugin.core.config.projectstructure.exception.NoChildModularityCaseFoundException;
import io.github.march_plugin.core.config.projectstructure.exception.UnequalCaseDimensionException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModularityTest {

    private Dimension layerDimension;
    private Dimension.Partition service;
    private Dimension.Partition web;
    private Dimension.Partition persistence;

    private Dimension teamDimension;
    private Dimension.Partition checkout;
    private Dimension.Partition search;

    private Dimension abstractionDimension;
    private Dimension.Partition api;
    private Dimension.Partition impl;

    private ModuleConvention moduleConvention;
    private PackageConvention packageConvention;

    @BeforeEach
    void setUp() {
        final var layerBuilder = new Dimension.Builder("layer");
        service = layerBuilder.addPartition("service");
        web = layerBuilder.addPartition("web");
        persistence = layerBuilder.addPartition("persistence");
        layerDimension = layerBuilder.build();

        final var teamBuilder = new Dimension.Builder("team");
        checkout = teamBuilder.addPartition("checkout");
        search = teamBuilder.addPartition("search");
        teamDimension = teamBuilder.build();

        final var abstractionBuilder = new Dimension.Builder("abstraction");
        api = abstractionBuilder.addPartition("api");
        impl = abstractionBuilder.addPartition("impl");
        abstractionDimension = abstractionBuilder.build();

        moduleConvention = new ModuleConvention.Builder()
                .setGroupId("io.github.march")
                .setArtifactId("app")
                .build();

        packageConvention = new PackageConvention("com.example");
    }

    private DimensionPartitionGroup groupOf(final Dimension.Partition... partitions) {
        final var builder = new DimensionPartitionGroup.Builder();
        for (final var p : partitions) {
            builder.addPartition(p);
        }
        return builder.build();
    }

    private ModuleModularity rootModuleModularity(final Dimension dimension) {
        return new ModuleModularity.Builder(dimension, moduleConvention).buildAsRoot();
    }

    private ModuleModularity teamChildModuleModularity(final ModuleModularity parent, final DimensionPartitionGroup casePartitions) {
        return new ModuleModularity.Builder(teamDimension, moduleConvention)
                .setCasePartitions(casePartitions)
                .buildAsChild(parent);
    }

    private PackageModularity childPackageModularity(final ModuleModularity parent, final Dimension dimension, final DimensionPartitionGroup casePartitions) {
        return new PackageModularity.Builder(dimension, packageConvention)
                .setCasePartitions(casePartitions)
                .buildAsChild(parent);
    }

    @Nested
    class RootModuleModularity {

        @Test
        void shouldBuildRootWithCorrectDimension() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getDimension()).isEqualTo(layerDimension);
        }

        @Test
        void rootShouldHaveNoParent() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getParent()).isEmpty();
        }

        @Test
        void rootShouldHaveNullCasePartitions() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getCasePartitions()).isNull();
        }

        @Test
        void rootShouldHaveNullAllowedPartitionsByDefault() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getAllowedPartitions()).isNull();
        }

        @Test
        void rootShouldStartWithNoChildren() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getChildren()).isEmpty();
            assertThat(root.getChildModules()).isEmpty();
            assertThat(root.getChildPackages()).isEmpty();
        }

        @Test
        void rootShouldStoreConvention() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getConvention()).isEqualTo(moduleConvention);
        }

        @Test
        void rootShouldRespectExplicitAllowedPartitions() {
            final var allowed = groupOf(service, web);
            final var root = new ModuleModularity.Builder(layerDimension, moduleConvention)
                    .setAllowedPartitions(allowed)
                    .buildAsRoot();
            assertThat(root.getAllowedPartitions()).isEqualTo(allowed);
        }

        @Test
        void rootWithNullDimensionShouldBuildSuccessfully() {
            final var root = new ModuleModularity.Builder(null, moduleConvention).buildAsRoot();
            assertThat(root.getDimension()).isNull();
        }
    }

    @Nested
    class SingleLevelChildModules {

        @Test
        void childShouldBeRegisteredOnParent() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));
            assertThat(root.getChildModules()).containsExactly(child);
        }

        @Test
        void childShouldHaveCorrectParent() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));
            assertThat(child.getParent()).contains(root);
        }

        @Test
        void childShouldHaveCorrectCasePartitions() {
            final var root = rootModuleModularity(layerDimension);
            final var caseGroup = groupOf(service, web);
            final var child = teamChildModuleModularity(root, caseGroup);
            assertThat(child.getCasePartitions()).isEqualTo(caseGroup);
        }

        @Test
        void multipleCasedChildrenShouldAllBeRegistered() {
            final var root = rootModuleModularity(layerDimension);
            final var childA = teamChildModuleModularity(root, groupOf(service));
            final var childB = teamChildModuleModularity(root, groupOf(web));
            assertThat(root.getChildModules()).containsExactlyInAnyOrder(childA, childB);
        }

        @Test
        void singleChildWithoutCasePartitionsShouldBeAllowed() {
            final var root = rootModuleModularity(layerDimension);
            new ModuleModularity.Builder(teamDimension, moduleConvention).buildAsChild(root);
            assertThat(root.getChildModules()).hasSize(1);
        }
    }

    @Nested
    class ConventionInheritance {

        @Test
        void childConventionShouldAppendGroupIdToParent() {
            final var root = rootModuleModularity(layerDimension);
            final var childOwnConvention = new ModuleConvention.Builder()
                    .setGroupId("plugin")
                    .setArtifactId("child-app")
                    .build();
            final var child = new ModuleModularity.Builder(teamDimension, childOwnConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThat(child.getConvention().getGroupId()).isEqualTo("io.github.march.plugin");
            assertThat(child.getConvention().getArtifactId()).isEqualTo("child-app");
        }

        @Test
        void childConventionShouldInheritParentArtifactIdWhenNotOverridden() {
            final var root = rootModuleModularity(layerDimension);
            final var childOwnConvention = new ModuleConvention.Builder()
                    .setGroupId("extra")
                    .build();
            final var child = new ModuleModularity.Builder(teamDimension, childOwnConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThat(child.getConvention().getArtifactId()).isEqualTo("app");
        }

        @Test
        void childConventionGroupIdShouldUseChildAloneWhenParentGroupIdIsEmpty() {
            final var emptyGroupConvention = new ModuleConvention.Builder()
                    .setGroupId("")
                    .setArtifactId("root-app")
                    .build();
            final var root = new ModuleModularity.Builder(layerDimension, emptyGroupConvention).buildAsRoot();
            final var childOwnConvention = new ModuleConvention.Builder()
                    .setGroupId("child")
                    .build();
            final var child = new ModuleModularity.Builder(teamDimension, childOwnConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThat(child.getConvention().getGroupId()).isEqualTo("child");
        }

        @Test
        void childConventionGroupIdShouldBeUnchangedWhenChildGroupIdIsNull() {
            final var root = rootModuleModularity(layerDimension);
            final var childOwnConvention = new ModuleConvention.Builder()
                    .setArtifactId("child-app")
                    .build();
            final var child = new ModuleModularity.Builder(teamDimension, childOwnConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThat(child.getConvention().getGroupId()).isEqualTo("io.github.march");
        }

        @Test
        void childConventionShouldTakeRootPackageFromChildConvention() {
            final var root = rootModuleModularity(layerDimension);
            final var rootPackage = new PackageHierarchy(List.of("io", "github"));
            final var childOwnConvention = new ModuleConvention.Builder()
                    .setRootPackage(rootPackage)
                    .build();
            final var child = new ModuleModularity.Builder(teamDimension, childOwnConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThat(child.getConvention().getRootPackage()).isEqualTo(rootPackage);
        }
    }

    @Nested
    class MultiLevelModuleHierarchy {

        @Test
        void grandchildShouldBeRegisteredOnDirectParentNotRoot() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));
            final var grandchild = new ModuleModularity.Builder(abstractionDimension, moduleConvention)
                    .setCasePartitions(groupOf(checkout))
                    .buildAsChild(child);

            assertThat(root.getChildModules()).containsExactly(child);
            assertThat(child.getChildModules()).containsExactly(grandchild);
        }

        @Test
        void getAllChildrenShouldReturnAllDescendants() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));
            final var grandchild = new ModuleModularity.Builder(abstractionDimension, moduleConvention)
                    .setCasePartitions(groupOf(checkout))
                    .buildAsChild(child);

            assertThat(root.getAllChildren()).containsExactlyInAnyOrder(child, grandchild);
        }

        @Test
        void getAllChildrenOnLeafShouldReturnEmpty() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));
            assertThat(child.getAllChildren()).isEmpty();
        }
    }

    @Nested
    class GetChildDispatch {

        @Test
        void getChildShouldReturnMatchingCasedChild() {
            final var root = rootModuleModularity(layerDimension);
            final var serviceChild = teamChildModuleModularity(root, groupOf(service));
            teamChildModuleModularity(root, groupOf(web));

            assertThat(root.getChild(service)).isEqualTo(serviceChild);
        }

        @Test
        void getChildShouldReturnSingleUncasedChildRegardlessOfPartition() {
            final var root = rootModuleModularity(layerDimension);
            final var only = new ModuleModularity.Builder(teamDimension, moduleConvention).buildAsChild(root);

            assertThat(root.getChild(service)).isEqualTo(only);
            assertThat(root.getChild(web)).isEqualTo(only);
        }

        @Test
        void getChildShouldThrowWhenNoMatchingCase() {
            final var root = rootModuleModularity(layerDimension);
            teamChildModuleModularity(root, groupOf(service));
            teamChildModuleModularity(root, groupOf(web));

            assertThatThrownBy(() -> root.getChild(persistence))
                    .isInstanceOf(NoChildModularityCaseFoundException.class);
        }

        @Test
        void getChildShouldMatchChildWhoseCaseGroupContainsMultiplePartitions() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service, web));

            assertThat(root.getChild(service)).isEqualTo(child);
            assertThat(root.getChild(web)).isEqualTo(child);
        }
    }

    @Nested
    class CaseDimensionValidation {

        @Test
        void childCasePartitionsFromWrongDimensionShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            final var wrongDimensionCase = groupOf(checkout);

            assertThatThrownBy(() ->
                    new ModuleModularity.Builder(teamDimension, moduleConvention)
                            .setCasePartitions(wrongDimensionCase)
                            .buildAsChild(root))
                    .isInstanceOf(UnequalCaseDimensionException.class);
        }

        @Test
        void secondChildWithWrongCaseDimensionShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            teamChildModuleModularity(root, groupOf(service));

            assertThatThrownBy(() ->
                    new ModuleModularity.Builder(teamDimension, moduleConvention)
                            .setCasePartitions(groupOf(checkout))
                            .buildAsChild(root))
                    .isInstanceOf(UnequalCaseDimensionException.class);
        }
    }

    @Nested
    class DuplicateCasePartitionValidation {

        @Test
        void twoChildrenWithOverlappingCasePartitionsShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            teamChildModuleModularity(root, groupOf(service));

            assertThatThrownBy(() -> teamChildModuleModularity(root, groupOf(service, web)))
                    .isInstanceOf(DuplicateCasePartitionException.class);
        }

        @Test
        void nonOverlappingCasePartitionsShouldSucceed() {
            final var root = rootModuleModularity(layerDimension);
            teamChildModuleModularity(root, groupOf(service));
            teamChildModuleModularity(root, groupOf(web));
            teamChildModuleModularity(root, groupOf(persistence));

            assertThat(root.getChildModules()).hasSize(3);
        }
    }

    @Nested
    class NoCaseForMultipleChildrenValidation {

        @Test
        void addingSecondUncasedChildShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            new ModuleModularity.Builder(teamDimension, moduleConvention).buildAsChild(root);

            assertThatThrownBy(() ->
                    new ModuleModularity.Builder(teamDimension, moduleConvention).buildAsChild(root))
                    .isInstanceOf(NoCaseDefinedForMultipleChildrenException.class);
        }

        @Test
        void addingCasedChildAfterUncasedChildShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            new ModuleModularity.Builder(teamDimension, moduleConvention).buildAsChild(root);

            assertThatThrownBy(() -> teamChildModuleModularity(root, groupOf(service)))
                    .isInstanceOf(NoCaseDefinedForMultipleChildrenException.class);
        }
    }

    @Nested
    class NullDimensionValidation {

        @Test
        void rootWithNullDimensionShouldThrowWhenChildIsAdded() {
            final var root = new ModuleModularity.Builder(null, moduleConvention).buildAsRoot();

            assertThatThrownBy(() ->
                    new ModuleModularity.Builder(layerDimension, moduleConvention).buildAsChild(root))
                    .isInstanceOf(EmptyModularityDimensionException.class);
        }
    }

    @Nested
    class PackageModularityAsChildOfModule {

        @Test
        void packageChildShouldBeRegisteredOnParent() {
            final var root = rootModuleModularity(layerDimension);
            final var pkg = childPackageModularity(root, teamDimension, groupOf(service));
            assertThat(root.getChildPackages()).containsExactly(pkg);
        }

        @Test
        void packageChildShouldHaveCorrectParentAndDimension() {
            final var root = rootModuleModularity(layerDimension);
            final var pkg = childPackageModularity(root, teamDimension, groupOf(service));

            assertThat(pkg.getParent()).contains(root);
            assertThat(pkg.getDimension()).isEqualTo(teamDimension);
        }

        @Test
        void packageChildShouldStoreConvention() {
            final var root = rootModuleModularity(layerDimension);
            final var pkg = childPackageModularity(root, teamDimension, groupOf(service));

            assertThat(pkg.getConvention()).isEqualTo(packageConvention);
        }

        @Test
        void multiplePackageChildrenShouldAllBeRegistered() {
            final var root = rootModuleModularity(layerDimension);
            final var pkgA = childPackageModularity(root, teamDimension, groupOf(service));
            final var pkgB = childPackageModularity(root, teamDimension, groupOf(web));

            assertThat(root.getChildPackages()).containsExactlyInAnyOrder(pkgA, pkgB);
        }

        @Test
        void packageChildrenShouldNotAppearInChildModules() {
            final var root = rootModuleModularity(layerDimension);
            childPackageModularity(root, teamDimension, groupOf(service));

            assertThat(root.getChildModules()).isEmpty();
        }
    }

    @Nested
    class PackageModularityNesting {

        @Test
        void packageCanHavePackageChild() {
            final var root = rootModuleModularity(layerDimension);
            final var parentPkg = new PackageModularity.Builder(teamDimension, packageConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var childPkg = new PackageModularity.Builder(abstractionDimension, packageConvention)
                    .setCasePartitions(groupOf(search))
                    .buildAsChild(parentPkg);

            assertThat(parentPkg.getChildPackages()).containsExactly(childPkg);
            assertThat(childPkg.getParent()).contains(parentPkg);
        }

        @Test
        void nestedPackageShouldAppearInGetAllChildrenOfRoot() {
            final var root = rootModuleModularity(layerDimension);
            final var parentPkg = new PackageModularity.Builder(teamDimension, packageConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var childPkg = new PackageModularity.Builder(abstractionDimension, packageConvention)
                    .setCasePartitions(groupOf(checkout))
                    .buildAsChild(parentPkg);

            assertThat(root.getAllChildren()).containsExactlyInAnyOrder(parentPkg, childPkg);
        }
    }

    @Nested
    class MixedChildTypeValidation {

        @Test
        void addingPackageChildAfterModuleChildShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            teamChildModuleModularity(root, groupOf(service));

            assertThatThrownBy(() -> childPackageModularity(root, teamDimension, groupOf(checkout)))
                    .isInstanceOf(DifferentChildModularityTypeException.class);
        }
    }

    @Nested
    class GetChildrenReturnType {

        @Test
        void getChildrenReturnsModuleListWhenModuleChildrenExist() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));

            assertThat(root.getChildModules()).containsExactly(child);
            assertThat(root.getChildPackages()).isEmpty();
        }

        @Test
        void getChildrenReturnsPackageListWhenOnlyPackageChildrenExist() {
            final var root = rootModuleModularity(layerDimension);
            final var pkg = childPackageModularity(root, teamDimension, groupOf(service));

            assertThat(root.getChildPackages()).containsExactly(pkg);
            assertThat(root.getChildModules()).isEmpty();
        }

        @Test
        void getChildrenReturnsEmptyWhenNoChildrenAdded() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getChildren()).isEmpty();
        }
    }

    @Nested
    class GetAllChildrenDeep {

        @Test
        void getAllChildrenOnRootWithNoChildrenReturnsEmpty() {
            final var root = rootModuleModularity(layerDimension);
            assertThat(root.getAllChildren()).isEmpty();
        }

        @Test
        void getAllChildrenShouldIncludeAllModuleDescendants() {
            final var root = rootModuleModularity(layerDimension);
            final var child = teamChildModuleModularity(root, groupOf(service));
            final var grandchild = new ModuleModularity.Builder(abstractionDimension, moduleConvention)
                    .setCasePartitions(groupOf(checkout))
                    .buildAsChild(child);

            assertThat(root.getAllChildren()).containsExactlyInAnyOrder(child, grandchild);
        }

        @Test
        void getAllChildrenShouldIncludeNestedPackageDescendants() {
            final var root = rootModuleModularity(layerDimension);
            final var pkg = childPackageModularity(root, teamDimension, groupOf(service));
            final var nestedPkg = new PackageModularity.Builder(abstractionDimension, packageConvention)
                    .setCasePartitions(groupOf(checkout))
                    .buildAsChild(pkg);

            assertThat(root.getAllChildren()).containsExactlyInAnyOrder(pkg, nestedPkg);
        }
    }

    @Nested
    class DuplicateDimensionInPathValidation {

        @Test
        void childWithSameDimensionAsParentShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            assertThatThrownBy(() -> new ModuleModularity.Builder(layerDimension, moduleConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root))
                    .isInstanceOf(DuplicateDimensionInPathException.class);
        }

        @Test
        void childWithDifferentDimensionThanParentShouldSucceed() {
            final var root = rootModuleModularity(layerDimension);
            final var child = new ModuleModularity.Builder(teamDimension, moduleConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThat(child.getDimension()).isEqualTo(teamDimension);
        }

        @Test
        void grandchildWithSameDimensionAsRootShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            final var child = new ModuleModularity.Builder(teamDimension, moduleConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThatThrownBy(() ->
                    new ModuleModularity.Builder(layerDimension, moduleConvention)
                            .setCasePartitions(groupOf(service))
                            .buildAsChild(child))
                    .isInstanceOf(DuplicateDimensionInPathException.class);
        }

        @Test
        void grandchildWithSameDimensionAsDirectParentShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            final var child = new ModuleModularity.Builder(teamDimension, moduleConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThatThrownBy(() ->
                    new ModuleModularity.Builder(teamDimension, moduleConvention)
                            .setCasePartitions(groupOf(search))
                            .buildAsChild(child))
                    .isInstanceOf(DuplicateDimensionInPathException.class);
        }

        @Test
        void packageChildWithSameDimensionAsParentModuleShouldThrow() {
            final var root = rootModuleModularity(layerDimension);

            assertThatThrownBy(() ->
                    new PackageModularity.Builder(layerDimension, packageConvention)
                            .setCasePartitions(groupOf(service))
                            .buildAsChild(root))
                    .isInstanceOf(DuplicateDimensionInPathException.class);
        }

        @Test
        void packageChildWithSameDimensionAsGrandparentShouldThrow() {
            final var root = rootModuleModularity(layerDimension);
            final var child = new ModuleModularity.Builder(teamDimension, moduleConvention)
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);

            assertThatThrownBy(() ->
                    new PackageModularity.Builder(layerDimension, packageConvention)
                            .setCasePartitions(groupOf(service))
                            .buildAsChild(child))
                    .isInstanceOf(DuplicateDimensionInPathException.class);
        }

        @Test
        void rootWithNullDimensionShouldNotThrow() {
            final var root = new ModuleModularity.Builder(null, moduleConvention).buildAsRoot();
            assertThat(root.getDimension()).isNull();
        }
    }
}