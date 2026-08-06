package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.modularity.ModuleModularityDto;
import io.github.march_plugin.configuration.dto.modularity.PackageModularityDto;
import io.github.march_plugin.configuration.dto.modularity.ProjectStructureDto;
import io.github.march_plugin.core.config.dimensions.exceptions.DimensionNotFoundException;
import io.github.march_plugin.core.config.dimensions.exceptions.GroupDuplicationPartitionDefinitionException;
import io.github.march_plugin.core.config.dimensions.exceptions.PartitionNotFoundException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.exception.DifferentChildModularityTypeException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectStructureInitializerTest {

    private Dimension layerDimension;
    private Dimension.Partition service;
    private Dimension.Partition web;

    private Dimension teamDimension;
    private Dimension.Partition checkout;
    private Dimension.Partition search;

    private Dimension abstractionDimension;

    private ProjectStructureInitializer initializer;

    @BeforeEach
    void setUp() {
        final var layerBuilder = new Dimension.Builder("layer");
        service = layerBuilder.addPartition("service");
        web = layerBuilder.addPartition("web");
        layerBuilder.addPartition("persistence");
        layerDimension = layerBuilder.build();

        final var teamBuilder = new Dimension.Builder("team");
        checkout = teamBuilder.addPartition("checkout");
        search = teamBuilder.addPartition("search");
        teamDimension = teamBuilder.build();

        final var abstractionBuilder = new Dimension.Builder("abstraction");
        abstractionBuilder.addPartition("api");
        abstractionBuilder.addPartition("impl");
        abstractionDimension = abstractionBuilder.build();

        final var dimensionRegistry = new DimensionRegistry.Builder()
                .addDimension(layerDimension)
                .addDimension(teamDimension)
                .addDimension(abstractionDimension)
                .build();

        initializer = new ProjectStructureInitializer(dimensionRegistry);
    }

    private static ModuleModularityDto moduleDto(final String dimension, final String pCase, final String allow,
                                                  final String groupId, final String artifactId, final String rootPackage,
                                                  final List<ModuleModularityDto> modules, final List<PackageModularityDto> packages) {
        return new ModuleModularityDto(dimension, pCase, allow, groupId, artifactId, rootPackage, modules, packages);
    }

    private static PackageModularityDto packageDto(final String dimension, final String pCase, final String allow,
                                                     final String name, final List<PackageModularityDto> packages) {
        return new PackageModularityDto(dimension, pCase, allow, name, packages);
    }

    @Nested
    class RootBuilding {

        @Test
        void shouldBuildRootWithDimensionAndConvention() {
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, null, null));

            final var root = initializer.build(dto);

            assertThat(root.getDimension()).isEqualTo(layerDimension);
            assertThat(root.getParent()).isEmpty();
            assertThat(root.getConvention().getGroupId()).isEqualTo("io.github");
            assertThat(root.getConvention().getArtifactId()).isEqualTo("app");
            assertThat(root.getConvention().getRootPackage()).isNull();
            assertThat(root.getChildren()).isEmpty();
        }

        @Test
        void shouldParseRootPackageIntoHierarchy() {
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", "io.github.app", null, null));

            final var root = initializer.build(dto);

            final var rootPackage = root.getConvention().getRootPackage();
            assertThat(rootPackage.depth()).isEqualTo(3);
            assertThat(rootPackage.toString()).isEqualTo("io.github.app");
        }

        @Test
        void shouldThrowWhenRootDimensionIsUnknown() {
            final var dto = new ProjectStructureDto(moduleDto("unknown", null, null, "io.github", "app", null, null, null));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(DimensionNotFoundException.class);
        }
    }

    @Nested
    class ChildModuleBuilding {

        @Test
        void shouldBuildSingleChildModuleWithoutCase() {
            final var child = moduleDto("team", null, null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules()).hasSize(1);
            final var builtChild = root.getChildModules().getFirst();
            assertThat(builtChild.getDimension()).isEqualTo(teamDimension);
            assertThat(builtChild.getCasePartitions()).isNull();
            assertThat(builtChild.getParent()).contains(root);
        }

        @Test
        void shouldResolveSinglePartitionCase() {
            final var child = moduleDto("team", "service", null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules().getFirst().getCasePartitions().getPartitions()).containsExactly(service);
        }

        @Test
        void shouldResolveMultiplePartitionsInCase() {
            final var child = moduleDto("team", "service;web", null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules().getFirst().getCasePartitions().getPartitions())
                    .containsExactlyInAnyOrder(service, web);
        }

        @Test
        void shouldBuildMultipleChildrenWithDistinctCasesAndDispatchByPartition() {
            final var serviceChild = moduleDto("team", "service", null, "svc", "svc-app", null, null, null);
            final var webChild = moduleDto("team", "web", null, "web", "web-app", null, null, null);
            final var dto = new ProjectStructureDto(
                    moduleDto("layer", null, null, "io.github", "app", null, List.of(serviceChild, webChild), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules()).hasSize(2);
            assertThat(((ModuleModularity) root.getChild(service)).getConvention().getArtifactId()).isEqualTo("svc-app");
            assertThat(((ModuleModularity) root.getChild(web)).getConvention().getArtifactId()).isEqualTo("web-app");
        }

        @Test
        void shouldResolveAllowedPartitionsFromOwnDimension() {
            final var child = moduleDto("team", null, "checkout", "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules().getFirst().getAllowedPartitions().getPartitions()).containsExactly(checkout);
        }

        @Test
        void shouldResolveMultiplePartitionsInAllow() {
            final var child = moduleDto("team", null, "checkout;search", "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules().getFirst().getAllowedPartitions().getPartitions())
                    .containsExactlyInAnyOrder(checkout, search);
        }

        @Test
        void shouldBuildChildWithNullDimensionWhenNotSpecified() {
            final var child = moduleDto(null, "service", null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules().getFirst().getDimension()).isNull();
        }

        @Test
        void shouldRegisterGrandchildOnDirectParentNotRoot() {
            final var grandchild = moduleDto("abstraction", "checkout", null, "impl", "impl-mod", null, null, null);
            final var child = moduleDto("team", "service", null, "child", "child-app", null, List.of(grandchild), null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            final var builtChild = root.getChildModules().getFirst();
            assertThat(root.getChildModules()).containsExactly(builtChild);
            assertThat(builtChild.getChildModules()).hasSize(1);
            assertThat(builtChild.getChildModules().getFirst().getDimension()).isEqualTo(abstractionDimension);
        }
    }

    @Nested
    class ConventionInheritance {

        @Test
        void shouldAppendChildGroupIdToParentGroupId() {
            final var child = moduleDto("team", null, null, "plugin", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            final var builtChild = root.getChildModules().getFirst();
            assertThat(builtChild.getConvention().getGroupId()).isEqualTo("io.github.plugin");
            assertThat(builtChild.getConvention().getArtifactId()).isEqualTo("child-app");
        }

        @Test
        void shouldInheritParentArtifactIdWhenChildDoesNotSpecifyOne() {
            final var child = moduleDto("team", null, null, "plugin", null, null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            assertThat(root.getChildModules().getFirst().getConvention().getArtifactId()).isEqualTo("app");
        }
    }

    @Nested
    class PackageModularityBuilding {

        @Test
        void shouldBuildPackageChildOfRoot() {
            final var pkg = packageDto("team", "service", null, "com.example", null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, null, List.of(pkg)));

            final var root = initializer.build(dto);

            assertThat(root.getChildPackages()).hasSize(1);
            final var builtPkg = root.getChildPackages().getFirst();
            assertThat(builtPkg.getDimension()).isEqualTo(teamDimension);
            assertThat(builtPkg.getConvention().packageName()).isEqualTo("com.example");
            assertThat(builtPkg.getCasePartitions().getPartitions()).containsExactly(service);
        }

        @Test
        void shouldBuildNestedPackageChildren() {
            final var nested = packageDto("abstraction", "checkout", null, "internal", null);
            final var pkg = packageDto("team", "service", null, "com.example", List.of(nested));
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, null, List.of(pkg)));

            final var root = initializer.build(dto);

            final var builtPkg = root.getChildPackages().getFirst();
            assertThat(builtPkg.getChildPackages()).hasSize(1);
            assertThat(builtPkg.getChildPackages().getFirst().getConvention().packageName()).isEqualTo("internal");
        }

        @Test
        void shouldBuildPackageChildOfModuleChild() {
            final var pkg = packageDto("abstraction", "checkout", null, "com.example", null);
            final var child = moduleDto("team", "service", null, "child", "child-app", null, null, List.of(pkg));
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            final var root = initializer.build(dto);

            final var builtChild = root.getChildModules().getFirst();
            assertThat(builtChild.getChildPackages()).hasSize(1);
        }

        @Test
        void shouldThrowWhenRootHasBothModuleAndPackageChildren() {
            final var child = moduleDto("team", null, null, "child", "child-app", null, null, null);
            final var pkg = packageDto("team", null, null, "com.example", null);
            final var dto = new ProjectStructureDto(
                    moduleDto("layer", null, null, "io.github", "app", null, List.of(child), List.of(pkg)));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(DifferentChildModularityTypeException.class);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldThrowWhenChildDimensionIsUnknown() {
            final var child = moduleDto("unknown", null, null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(DimensionNotFoundException.class);
        }

        @Test
        void shouldThrowWhenCasePartitionIsUnknown() {
            final var child = moduleDto("team", "unknown-partition", null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(PartitionNotFoundException.class);
        }

        @Test
        void shouldThrowWhenAllowPartitionIsUnknown() {
            final var child = moduleDto("team", null, "unknown-partition", "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(PartitionNotFoundException.class);
        }

        @Test
        void shouldThrowWhenCasePartitionIsDuplicated() {
            final var child = moduleDto("team", "service;service", null, "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(GroupDuplicationPartitionDefinitionException.class);
        }

        @Test
        void shouldThrowNullPointerWhenAllowIsSpecifiedWithoutOwnDimension() {
            final var child = moduleDto(null, null, "checkout", "child", "child-app", null, null, null);
            final var dto = new ProjectStructureDto(moduleDto("layer", null, null, "io.github", "app", null, List.of(child), null));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
