package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.modularity.ModuleModularityDto;
import io.github.march_plugin.configuration.dto.modularity.PackageModularityDto;
import io.github.march_plugin.configuration.dto.modularity.ProjectStructureDto;
import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionPartitionGroup;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;

import java.util.Arrays;
import java.util.List;

/**
 * Builds the {@link ModuleModularity} tree describing the project structure from the March configuration,
 * resolving dimension and partition references against a {@link DimensionRegistry}.
 */
public class ProjectStructureInitializer {

    private final DimensionRegistry dimensionRegistry;

    /**
     * Constructs the initializer.
     *
     * @param dimensionRegistry the registry used to resolve the dimensions and partitions referenced in the configuration
     */
    public ProjectStructureInitializer(final DimensionRegistry dimensionRegistry) {
        this.dimensionRegistry = dimensionRegistry;
    }

    /**
     * Builds the module modularity tree from the given project structure configuration.
     *
     * @param projectStructureDto the project structure declared in the March configuration
     * @return the root of the built module modularity tree
     */
    public ModuleModularity build(final ProjectStructureDto projectStructureDto) {
        final var rootDto = projectStructureDto.modularity();

        final var dimension = dimensionRegistry.getDimension(rootDto.dimension());

        final var rootPackage = rootDto.rootPackage() == null ? null : new PackageHierarchy(Arrays.stream(rootDto.rootPackage().split("\\.")).toList());
        final var root = new ModuleModularity.Builder(
                dimension,
                new ModuleConvention.Builder()
                        .setGroupId(rootDto.groupId())
                        .setArtifactId(rootDto.artifactId())
                        .setRootPackage(rootPackage)
                        .build())
                .buildAsRoot();


        initModuleModularity(root, rootDto.packageModularity(), rootDto.modularity());

        return root;
    }

    private void initModuleModularity(final ModuleModularity parent, final List<PackageModularityDto> packageModularityDtoChildren, final List<ModuleModularityDto> moduleModularityDtoChildren) {
        if (moduleModularityDtoChildren != null) {
            for (final var childDto : moduleModularityDtoChildren) {
                final var child = initModuleModularityBuilder(childDto, parent.getDimension()).buildAsChild(parent);
                initModuleModularity(child, childDto.packageModularity(), childDto.modularity());

            }
        }

        initPackageModularity(parent, packageModularityDtoChildren);
    }

    private ModuleModularity.Builder initModuleModularityBuilder(final ModuleModularityDto moduleModularityDto, final Dimension parentDimension) {
        final var dimension = moduleModularityDto.dimension() == null ? null : dimensionRegistry.getDimension(moduleModularityDto.dimension());
        final var dimensionPartitionGroup = buildCase(moduleModularityDto.getCase(), parentDimension);
        final var allowGroup = buildAllow(moduleModularityDto.getAllow(), dimension);

        final var rootPackage = moduleModularityDto.rootPackage() == null ? null : new PackageHierarchy(Arrays.stream(moduleModularityDto.rootPackage().split("\\.")).toList());
        return new ModuleModularity.Builder(dimension,
                new ModuleConvention.Builder()
                        .setGroupId(moduleModularityDto.groupId())
                        .setArtifactId(moduleModularityDto.artifactId())
                        .setRootPackage(rootPackage)
                        .build())
                .setCasePartitions(dimensionPartitionGroup)
                .setAllowedPartitions(allowGroup);
    }

    private void initPackageModularity(final Modularity parent, final List<PackageModularityDto> packageModularityDtoChildren) {
        if (packageModularityDtoChildren != null) {
            for (final var childDto : packageModularityDtoChildren) {
                final var child = initPackageModularityBuilder(childDto, parent.getDimension()).buildAsChild(parent);
                initPackageModularity(child, childDto.getPackageModularity());
            }
        }
    }

    private PackageModularity.Builder initPackageModularityBuilder(final PackageModularityDto modularityDto, final Dimension parentDimension) {
        final var dimension = modularityDto.dimension() == null ? null : dimensionRegistry.getDimension(modularityDto.dimension());
        final var dimensionPartitionGroup = buildCase(modularityDto.getCase(), parentDimension);
        final var allowGroup = buildAllow(modularityDto.getAllow(), dimension);

        return new PackageModularity.Builder(dimension, new PackageConvention(modularityDto.name()))
                .setCasePartitions(dimensionPartitionGroup)
                .setAllowedPartitions(allowGroup);
    }

    private DimensionPartitionGroup buildCase(final String pCase, final Dimension parentDimension) {
        if (pCase != null && parentDimension != null) {
            final var dimensionPartitionGroupBuilder = new DimensionPartitionGroup.Builder();
            for (final var partition : pCase.split(";")) {
                dimensionPartitionGroupBuilder.addPartition(parentDimension.getPartition(partition));
            }
            return dimensionPartitionGroupBuilder.build();
        }
        return null;
    }

    private DimensionPartitionGroup buildAllow(final String allow, final Dimension dimension) {
        if (allow != null) {
            final var dimensionPartitionGroupBuilder = new DimensionPartitionGroup.Builder();
            for (final var partition : allow.split(";")) {
                dimensionPartitionGroupBuilder.addPartition(dimension.getPartition(partition));
            }
            return dimensionPartitionGroupBuilder.build();
        }
        return null;
    }
}
