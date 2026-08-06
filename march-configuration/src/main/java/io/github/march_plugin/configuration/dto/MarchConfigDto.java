package io.github.march_plugin.configuration.dto;

import io.github.march_plugin.configuration.dto.dimensions.DimensionDto;

import java.util.List;

public record MarchConfigDto(
        List<DimensionDto> dimensions
) {}
