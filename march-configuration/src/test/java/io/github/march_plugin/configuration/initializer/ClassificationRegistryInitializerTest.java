package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.classification.ModuleDto;
import io.github.march_plugin.configuration.dto.classification.PackageTemplateRefDto;
import io.github.march_plugin.configuration.dto.classification.VirtualModuleDto;
import io.github.march_plugin.configuration.dto.classification.VirtualModuleRefDto;
import io.github.march_plugin.configuration.initializer.exception.ConflictingModuleChildrenException;
import io.github.march_plugin.configuration.initializer.exception.MissingRootGroupIdException;
import io.github.march_plugin.core.config.classification.exception.ComponentPartitionNotDefinedException;
import io.github.march_plugin.core.config.classification.model.ClassifiedConcreteModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModuleReference;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.config.dimensions.exceptions.PartitionNotFoundException;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.package_templates.exception.PackageTemplateNotFoundException;
import io.github.march_plugin.core.config.package_templates.model.JPackage;
import io.github.march_plugin.core.config.package_templates.model.PackageTemplateRegistry;
import io.github.march_plugin.core.config.projectstructure.exception.NoChildModularityCaseFoundException;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassificationRegistryInitializerTest {

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
    }

    private DimensionPartitionGroup groupOf(final Dimension.Partition... partitions) {
        final var builder = new DimensionPartitionGroup.Builder();
        for (final var partition : partitions) {
            builder.addPartition(partition);
        }
        return builder.build();
    }

    private static ModuleConvention emptyModuleConvention() {
        return new ModuleConvention.Builder().build();
    }

    private static PackageConvention emptyPackageConvention() {
        return new PackageConvention(null);
    }

    private static PackageTemplateRegistry emptyTemplateRegistry() {
        return new PackageTemplateRegistry.Builder().build();
    }

    private static PackageTemplateRegistry templateRegistryWith(final String name, final JPackage root) {
        final var builder = new PackageTemplateRegistry.Builder();
        builder.addPackageTemplate(name, root);
        return builder.build();
    }

    private static JPackage jpackage(final String simpleName, final String partition, final boolean optional, final List<JPackage> children) {
        return new JPackage(new PackageHierarchy(List.of(simpleName)), partition, optional, children);
    }

    private static ModuleDto moduleDto(final List<ModuleDto> modules, final PackageTemplateRefDto packageTemplate, final List<VirtualModuleDto> virtualModules,
                                        final String groupId, final String artifactId, final String partition, final String rootPackage) {
        return new ModuleDto(modules, packageTemplate, virtualModules, groupId, artifactId, partition, rootPackage);
    }

    private static VirtualModuleDto virtualModuleDto(final List<VirtualModuleDto> virtualModules, final List<VirtualModuleRefDto> virtualModuleRefs,
                                                       final String partition, final String groupId, final String virtualArtifactId, final String virtualGroupId) {
        return new VirtualModuleDto(virtualModules, virtualModuleRefs, partition, groupId, virtualArtifactId, virtualGroupId);
    }

    private static VirtualModuleRefDto virtualModuleRefDto(final String groupId, final String artifactId, final String partition,
                                                             final String virtualArtifactId, final String virtualGroupId) {
        return new VirtualModuleRefDto(groupId, artifactId, partition, virtualArtifactId, virtualGroupId);
    }

    private static ClassifiedPackage findByHierarchy(final Collection<ClassifiedPackage> packages, final String hierarchy) {
        return packages.stream()
                .filter(p -> p.getPackageHierarchy().toString().equals(hierarchy))
                .findFirst()
                .orElseThrow();
    }

    @Nested
    class RootModuleBuilding {

        @Test
        void shouldBuildRootModuleAndRegisterIt() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var rootDto = moduleDto(null, null, null, "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var classifiedModule = (ClassifiedConcreteModule) registry.getClassifiedModule(new ModuleCoordinates("io.example", "app"));
            assertThat(classifiedModule.getPartition()).isNull();
        }

        @Test
        void shouldBuildRootWithoutRootPackage() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var rootDto = moduleDto(null, null, null, "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var classifiedModule = (ClassifiedConcreteModule) registry.getClassifiedModule(new ModuleCoordinates("io.example", "app"));
            assertThat(classifiedModule.getRootPackage()).isNull();
        }

        @Test
        void shouldThrowWhenRootGroupIdIsMissing() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var rootDto = moduleDto(null, null, null, null, "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(MissingRootGroupIdException.class);
        }
    }

    @Nested
    class ChildModuleBuilding {

        @Test
        void shouldClassifyChildModuleWithInheritedGroupId() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var childDto = moduleDto(null, null, null, null, "svc", "service", null);
            final var rootDto = moduleDto(List.of(childDto), null, null, "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var classifiedChild = (ClassifiedConcreteModule) registry.getClassifiedModule(new ModuleCoordinates("io.example", "svc"));
            assertThat(classifiedChild.getPartition()).isEqualTo(service);
        }

        @Test
        void shouldClassifyChildModuleWithOwnGroupId() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var childDto = moduleDto(null, null, null, "io.other", "svc", "service", null);
            final var rootDto = moduleDto(List.of(childDto), null, null, "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            assertThat(registry.getClassifiedModule(new ModuleCoordinates("io.other", "svc"))).isNotNull();
        }

        @Test
        void shouldBuildGrandchildModuleWithNestedGroupIdInheritance() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var serviceModularity = new ModuleModularity.Builder(abstractionDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            new ModuleModularity.Builder(null, emptyModuleConvention())
                    .setCasePartitions(groupOf(api))
                    .buildAsChild(serviceModularity);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var grandchildDto = moduleDto(null, null, null, null, "grand", "api", null);
            final var childDto = moduleDto(List.of(grandchildDto), null, null, "io.child", "svc", "service", null);
            final var rootDto = moduleDto(List.of(childDto), null, null, "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var classifiedGrand = (ClassifiedConcreteModule) registry.getClassifiedModule(new ModuleCoordinates("io.child", "grand"));
            assertThat(classifiedGrand.getPartition()).isEqualTo(api);
        }

        @Test
        void shouldThrowWhenChildPartitionNameIsUnknown() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var childDto = moduleDto(null, null, null, null, "svc", "unknown", null);
            final var rootDto = moduleDto(List.of(childDto), null, null, "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(PartitionNotFoundException.class);
        }

        @Test
        void shouldThrowWhenNoModularityCaseMatchesPartition() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var childDto = moduleDto(null, null, null, null, "svc", "persistence", null);
            final var rootDto = moduleDto(List.of(childDto), null, null, "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(NoChildModularityCaseFoundException.class);
        }
    }

    @Nested
    class VirtualModuleBuilding {

        @Test
        void shouldBuildVirtualModuleAndRegisterIt() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var virtualDto = virtualModuleDto(null, null, "persistence", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var classifiedVirtual = registry.getClassifiedModule(new ModuleCoordinates("io.example", "legacy-lib"));
            assertThat(classifiedVirtual).isInstanceOf(ClassifiedVirtualModule.class);
            assertThat(classifiedVirtual.getPartition()).isEqualTo(persistence);
        }

        @Test
        void shouldInheritGroupIdWhenVirtualGroupIdIsMissing() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var virtualDto = virtualModuleDto(null, null, "persistence", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            assertThat(registry.getClassifiedModule(new ModuleCoordinates("io.example", "legacy-lib"))).isNotNull();
        }

        @Test
        void shouldOverrideGroupIdWithVirtualGroupId() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var virtualDto = virtualModuleDto(null, null, "persistence", null, "legacy-lib", "io.legacy");
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            assertThat(registry.getClassifiedModule(new ModuleCoordinates("io.legacy", "legacy-lib"))).isNotNull();
        }

        @Test
        void shouldBuildNestedVirtualModules() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var persistenceModularity = new ModuleModularity.Builder(abstractionDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            new ModuleModularity.Builder(null, emptyModuleConvention())
                    .setCasePartitions(groupOf(api))
                    .buildAsChild(persistenceModularity);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var nestedVirtualDto = virtualModuleDto(null, null, "api", null, "nested-lib", null);
            final var virtualDto = virtualModuleDto(List.of(nestedVirtualDto), null, "persistence", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var nested = registry.getClassifiedModule(new ModuleCoordinates("io.example", "nested-lib"));
            assertThat(nested).isInstanceOf(ClassifiedVirtualModule.class);
            assertThat(nested.getPartition()).isEqualTo(api);
        }

        @Test
        void shouldThrowWhenVirtualModulePartitionIsUnknown() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var virtualDto = virtualModuleDto(null, null, "unknown", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(PartitionNotFoundException.class);
        }
    }

    @Nested
    class VirtualModuleRefBuilding {

        @Test
        void shouldBuildVirtualModuleRefWithExternalCoordinates() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var persistenceModularity = new ModuleModularity.Builder(abstractionDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            new ModuleModularity.Builder(null, emptyModuleConvention())
                    .setCasePartitions(groupOf(api))
                    .buildAsChild(persistenceModularity);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var refDto = virtualModuleRefDto("io.external", "ext-artifact", "api", "internal-artifact", null);
            final var virtualDto = virtualModuleDto(null, List.of(refDto), "persistence", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var internalCoords = new ModuleCoordinates("io.example", "internal-artifact");
            final var classifiedRef = (ClassifiedVirtualModuleReference) registry.getClassifiedModule(internalCoords);
            assertThat(classifiedRef.getExternalCoordinates()).isEqualTo(new ModuleCoordinates("io.external", "ext-artifact"));
            assertThat(registry.getClassifiedModule(new ModuleCoordinates("io.external", "ext-artifact"))).isEqualTo(classifiedRef);
        }

        @Test
        void shouldOverrideGroupIdWithRefVirtualGroupId() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var persistenceModularity = new ModuleModularity.Builder(abstractionDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            new ModuleModularity.Builder(null, emptyModuleConvention())
                    .setCasePartitions(groupOf(api))
                    .buildAsChild(persistenceModularity);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var refDto = virtualModuleRefDto("io.external", "ext-artifact", "api", "internal-artifact", "io.internal");
            final var virtualDto = virtualModuleDto(null, List.of(refDto), "persistence", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            assertThat(registry.getClassifiedModule(new ModuleCoordinates("io.internal", "internal-artifact"))).isNotNull();
        }

        @Test
        void shouldThrowWhenVirtualModuleRefPartitionIsUnknown() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            final var persistenceModularity = new ModuleModularity.Builder(abstractionDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(persistence))
                    .buildAsChild(root);
            new ModuleModularity.Builder(null, emptyModuleConvention())
                    .setCasePartitions(groupOf(api))
                    .buildAsChild(persistenceModularity);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var refDto = virtualModuleRefDto("io.external", "ext-artifact", "unknown", "internal-artifact", null);
            final var virtualDto = virtualModuleDto(null, List.of(refDto), "persistence", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, null, List.of(virtualDto), "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(PartitionNotFoundException.class);
        }
    }

    @Nested
    class PackageTemplateClassification {

        private ModuleModularity root;
        private ModuleModularity webModularity;

        @BeforeEach
        void setUpTree() {
            root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            webModularity = new ModuleModularity.Builder(abstractionDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(web))
                    .buildAsChild(root);

            final var apiPackageModularity = new PackageModularity.Builder(teamDimension, emptyPackageConvention())
                    .setCasePartitions(groupOf(api))
                    .buildAsChild(webModularity);
            new PackageModularity.Builder(null, emptyPackageConvention())
                    .setCasePartitions(groupOf(checkout))
                    .buildAsChild(apiPackageModularity);
            new PackageModularity.Builder(null, emptyPackageConvention())
                    .setCasePartitions(groupOf(impl))
                    .buildAsChild(webModularity);
        }

        @Test
        void shouldClassifyTopLevelAndNestedPackagesFromTemplate() {
            final var nestedTemplate = jpackage("checkout", "checkout", true, null);
            final var apiTemplate = jpackage("api", "api", false, List.of(nestedTemplate));
            final var implTemplate = jpackage("impl", "impl", false, null);
            final var templateRoot = new JPackage(null, null, false, List.of(apiTemplate, implTemplate));
            final var templateRegistry = templateRegistryWith("standard", templateRoot);

            final var initializer = new ClassificationRegistryInitializer(root, templateRegistry);
            final var webDto = moduleDto(null, new PackageTemplateRefDto("standard"), null, null, "web-module", "web", "webmod");
            final var rootDto = moduleDto(List.of(webDto), null, null, "io.example", "app", null, null);

            final var registry = initializer.build(rootDto);

            final var packages = registry.getAllClassifiedPackages();
            assertThat(packages).hasSize(3);

            final var apiPkg = findByHierarchy(packages, "webmod.api");
            assertThat(apiPkg.getPartition()).isEqualTo(api);
            assertThat(apiPkg.isOptional()).isFalse();

            final var checkoutPkg = findByHierarchy(packages, "webmod.api.checkout");
            assertThat(checkoutPkg.getPartition()).isEqualTo(checkout);
            assertThat(checkoutPkg.isOptional()).isTrue();

            final var implPkg = findByHierarchy(packages, "webmod.impl");
            assertThat(implPkg.getPartition()).isEqualTo(impl);
        }

        @Test
        void shouldThrowWhenPackageTemplateJPackageHasNoPartition() {
            final var badTemplate = jpackage("api", null, false, null);
            final var templateRoot = new JPackage(null, null, false, List.of(badTemplate));
            final var templateRegistry = templateRegistryWith("bad", templateRoot);

            final var initializer = new ClassificationRegistryInitializer(root, templateRegistry);
            final var webDto = moduleDto(null, new PackageTemplateRefDto("bad"), null, null, "web-module", "web", "webmod");
            final var rootDto = moduleDto(List.of(webDto), null, null, "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(ComponentPartitionNotDefinedException.class);
        }

        @Test
        void shouldThrowWhenPackageTemplateNameIsUnknown() {
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());
            final var webDto = moduleDto(null, new PackageTemplateRefDto("missing"), null, null, "web-module", "web", "webmod");
            final var rootDto = moduleDto(List.of(webDto), null, null, "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(PackageTemplateNotFoundException.class);
        }
    }

    @Nested
    class MutualExclusivityValidation {

        @Test
        void shouldThrowWhenModuleHasBothChildModulesAndPackageTemplate() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var childDto = moduleDto(null, null, null, null, "child", "service", null);
            final var rootDto = moduleDto(List.of(childDto), new PackageTemplateRefDto("x"), null, "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(ConflictingModuleChildrenException.class);
        }

        @Test
        void shouldThrowWhenModuleHasBothVirtualModulesAndPackageTemplate() {
            final var root = new ModuleModularity.Builder(layerDimension, emptyModuleConvention()).buildAsRoot();
            new ModuleModularity.Builder(teamDimension, emptyModuleConvention())
                    .setCasePartitions(groupOf(service))
                    .buildAsChild(root);
            final var initializer = new ClassificationRegistryInitializer(root, emptyTemplateRegistry());

            final var virtualDto = virtualModuleDto(null, null, "service", null, "legacy-lib", null);
            final var rootDto = moduleDto(null, new PackageTemplateRefDto("x"), List.of(virtualDto), "io.example", "app", null, null);

            assertThatThrownBy(() -> initializer.build(rootDto))
                    .isInstanceOf(ConflictingModuleChildrenException.class);
        }
    }
}
