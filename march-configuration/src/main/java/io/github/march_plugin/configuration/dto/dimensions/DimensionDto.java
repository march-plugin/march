package io.github.march_plugin.configuration.dto.dimensions;

public record DimensionDto(
        String name,
        String description,
        PartitionsDto partitions
) {}