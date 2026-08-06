package io.github.march_plugin.configuration.dto.classification;

public record VirtualModuleRefDto(
        String groupId,
        String artifactId,
        String partition,
        String virtualArtifactId,
        String virtualGroupId
) {
}
