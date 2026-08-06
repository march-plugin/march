package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.classification.ModuleDto;
import io.github.march_plugin.configuration.dto.classification.VirtualModuleDto;
import io.github.march_plugin.configuration.dto.classification.VirtualModuleRefDto;
import io.github.march_plugin.configuration.initializer.exception.ConflictingModuleChildrenException;
import io.github.march_plugin.configuration.initializer.exception.MissingRootGroupIdException;
import io.github.march_plugin.core.config.classification.exception.ComponentPartitionNotDefinedException;
import io.github.march_plugin.core.config.classification.model.ClassificationRegistry;
import io.github.march_plugin.core.config.classification.model.ClassifiedComponent;
import io.github.march_plugin.core.config.classification.model.ClassifiedConcreteModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedPackage;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModule;
import io.github.march_plugin.core.config.classification.model.ClassifiedVirtualModuleReference;
import io.github.march_plugin.core.config.classification.model.ModuleCoordinates;
import io.github.march_plugin.core.config.package_templates.model.JPackage;
import io.github.march_plugin.core.config.package_templates.model.PackageTemplateRegistry;
import io.github.march_plugin.core.config.projectstructure.model.Modularity;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;

import java.util.Arrays;

public class ClassificationRegistryInitializer {

    private final PackageTemplateRegistry packageTemplateRegistry;
    private final ModuleModularity projectStructureRoot;
    private final ClassificationRegistry.Builder builder;

    /**
     * Constructs the initializer.
     *
     * @param projectStructureRoot the root of the module modularity tree describing the allowed project structure
     * @param packageTemplateRegistry the registry used to resolve package templates referenced in the configuration
     */
    public ClassificationRegistryInitializer(final ModuleModularity projectStructureRoot, final PackageTemplateRegistry packageTemplateRegistry) {
        this.projectStructureRoot = projectStructureRoot;
        this.packageTemplateRegistry = packageTemplateRegistry;
        this.builder = new ClassificationRegistry.Builder();
    }

    /**
     * Builds the classification registry by classifying the modules, virtual modules and packages declared
     * in the configuration against the project structure.
     *
     * @param rootModuleDto the root module declared in the March configuration
     * @return the built classification registry
     */
    public ClassificationRegistry build(final ModuleDto rootModuleDto) {
        if (rootModuleDto.groupId() == null) {
            throw new MissingRootGroupIdException();
        }
        registerModule(projectStructureRoot, null, rootModuleDto, rootModuleDto.groupId());
        return builder.build();
    }


    private void registerModule(final ModuleModularity modularity, final ClassifiedConcreteModule parent, final ModuleDto moduleDto, final String inheritedGroupId) {
        final var groupId = moduleDto.groupId() == null ? inheritedGroupId : moduleDto.groupId();
        final var artifactId = moduleDto.artifactId();

        final var partition = modularity.getParent().isPresent() ? modularity.getParent().get().getDimension().getPartition(moduleDto.partition()) : null;
        final var coordinates = new ModuleCoordinates(groupId, artifactId);


        final var rootPackage = moduleDto.rootPackage() == null ? null : new PackageHierarchy(Arrays.stream(moduleDto.rootPackage().split("\\.")).toList());
        final var classifiedModuleBuilder = new ClassifiedConcreteModule.Builder(coordinates, partition)
                .setRootPackage(rootPackage);

        final var classifiedModule = parent == null ? classifiedModuleBuilder.buildAsRoot() : classifiedModuleBuilder.buildAsChild(parent, modularity);

        builder.addModuleClassification(classifiedModule);

        if (moduleDto.module() != null) {
            for (final var child : moduleDto.module()) {
                final var childModule = (ModuleModularity) modularity.getChild(modularity.getDimension().getPartition(child.partition()));

                registerModule(childModule, classifiedModule, child, groupId);
            }
        }
        if (moduleDto.virtualModule() != null) {
            for (final var child : moduleDto.virtualModule()) {
                final var childModule = (ModuleModularity) modularity.getChild(modularity.getDimension().getPartition(child.partition()));
                registerVirtualModule(childModule, classifiedModule, child, groupId);
            }
        }


        if (moduleDto.packageTemplate() != null) {
            if (moduleDto.module() != null || moduleDto.virtualModule() != null) {
                throw new ConflictingModuleChildrenException(coordinates.toString());
            }

            final var packageTemplate = packageTemplateRegistry.getPackageTemplate(moduleDto.packageTemplate().name());
            classifyPackageTemplate(classifiedModule, modularity, packageTemplate);

        }
    }

    private void classifyPackageTemplate(final ClassifiedConcreteModule classifiedConcreteModule, final ModuleModularity modularity, final JPackage packageTemplate) {
        for (final var jpackage : packageTemplate.children()) {
            classifyPackage(modularity, jpackage, classifiedConcreteModule);
        }
    }

    private void classifyPackage(final Modularity parentModularity, final JPackage jPackage, final ClassifiedComponent parentComponent) {

        final PackageHierarchy parentHierarchy;
        if (parentComponent instanceof ClassifiedConcreteModule classifiedConcreteModule) {
            parentHierarchy = classifiedConcreteModule.getRootPackage();
        } else if (parentComponent instanceof ClassifiedPackage classifiedPackage) {
            parentHierarchy = classifiedPackage.getPackageHierarchy();
        } else {
            throw new IllegalArgumentException("Unexpected type");
        }

        final var packageHierarchy = PackageHierarchy.buildChild(parentHierarchy, jPackage.packageHierarchy().getSimpleName());

        if (jPackage.partition() == null) {
            throw new ComponentPartitionNotDefinedException(parentComponent.getModuleCoordinates() + ":" + packageHierarchy);
        }
        final var partition = parentModularity.getDimension().getPartition(jPackage.partition());
        final var classifiedPackageBuilder = new ClassifiedPackage.Builder(parentComponent.getModuleCoordinates(), partition, packageHierarchy);

        if (jPackage.optional() != null && jPackage.optional()) {
            classifiedPackageBuilder.setOptional();
        }

        final var modularity = (PackageModularity) parentModularity.getChild(parentModularity.getDimension().getPartition(jPackage.partition()));
        final var classifiedPackage = classifiedPackageBuilder.buildAsChild(parentComponent, modularity);

        builder.addPackageClassification(classifiedPackage);


        if (jPackage.children() != null) {
            for (final var child : jPackage.children()) {
                classifyPackage(modularity, child, classifiedPackage);
            }
        }
    }

    private void registerVirtualModule(final ModuleModularity modularity, final ClassifiedModule parent, final VirtualModuleDto moduleDto, final String inheritedGroupId) {
        final var partition = modularity.getParent().get().getDimension().getPartition(moduleDto.partition());
        final var groupId = moduleDto.virtualGroupId() == null ? inheritedGroupId : moduleDto.virtualGroupId();
        final var virtualCoordinates = new ModuleCoordinates(groupId, moduleDto.virtualArtifactId());

        final var classificationBuilder = new ClassifiedVirtualModule.Builder(virtualCoordinates, partition);
        final var classification = classificationBuilder.buildAsChild(parent, modularity);
        builder.addModuleClassification(classification);

        if (moduleDto.virtualModule() != null) {
            for (final var child : moduleDto.virtualModule()) {
                final var childModule = (ModuleModularity) modularity.getChild(modularity.getDimension().getPartition(child.partition()));
                registerVirtualModule(childModule, classification, child, groupId);
            }
        }
        if (moduleDto.virtualModuleRef() != null) {
            for (final var child : moduleDto.virtualModuleRef()) {
                final var childModule = (ModuleModularity) modularity.getChild(modularity.getDimension().getPartition(child.partition()));
                registerVirtualModuleRef(childModule, classification, child, groupId);
            }
        }
    }

    private void registerVirtualModuleRef(final ModuleModularity modularity, final ClassifiedModule parent, final VirtualModuleRefDto virtualModuleRefDto, final String inheritedGroupId) {
        final var virtualGroupId = virtualModuleRefDto.virtualGroupId() == null ? inheritedGroupId : virtualModuleRefDto.virtualGroupId();
        final var partition = modularity.getParent().get().getDimension().getPartition(virtualModuleRefDto.partition());

        final var virtualCoordinates = new ModuleCoordinates(virtualGroupId, virtualModuleRefDto.virtualArtifactId());
        final var externalCoordinates = new ModuleCoordinates(virtualModuleRefDto.groupId(), virtualModuleRefDto.artifactId());
        final var classificationBuilder = new ClassifiedVirtualModuleReference.Builder(virtualCoordinates, partition, externalCoordinates);
        final var classification = classificationBuilder.buildAsChild(parent, modularity);
        builder.addModuleClassification(classification);
    }
}
