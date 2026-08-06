package io.github.march_plugin.configuration.dto.modularity;

import java.util.List;

/**
 * Common configuration shared by module and package modularity declarations.
 */
public interface ModularityDto {

    /**
     * Gets the name of the dimension that children of this modularity must classify.
     *
     * @return the dimension name, or {@code null} if this modularity has no dimension
     */
    String getDimension();

    /**
     * Gets the partitions of the parent's dimension that select this modularity among its siblings.
     *
     * @return the partition names separated by {@code ;}, or {@code null} if this is the only child
     */
    String getCase();

    /**
     * Gets the partitions of this modularity's own dimension that its children are restricted to.
     *
     * @return the partition names separated by {@code ;}, or {@code null} if all partitions are allowed
     */
    String getAllow();

    /**
     * Gets the package modularity declared as children of this modularity.
     *
     * @return the child package modularities, or {@code null} if none are declared
     */
    List<PackageModularityDto> getPackageModularity();
}