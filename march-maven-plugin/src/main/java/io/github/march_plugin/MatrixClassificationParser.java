package io.github.march_plugin;

import io.github.march_plugin.core.config.dimensions.model.Dimension;
import io.github.march_plugin.core.config.dimensions.model.DimensionRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses the {@code -Dclassifications} matrix input syntax, e.g. {@code "{domain(article;order);layer}"},
 * into the partitions each requested dimension should be evaluated with.
 */
public class MatrixClassificationParser {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("([^.({\\s]+)(?:\\.?[({]([^)}]+)[)}])?");

    /**
     * Parses the given input, or defaults to every dimension in {@code dimensionRegistry} with all of its
     * partitions when {@code input} is null or empty.
     *
     * @param input the raw {@code -Dclassifications} value, or null/empty to use every dimension
     * @param dimensionRegistry the dimensions available to resolve names and partitions against
     * @return one partition set per requested dimension
     */
    public List<Set<Dimension.Partition>> parse(final String input, final DimensionRegistry dimensionRegistry) {
        if (input == null || input.isEmpty()) {
            return dimensionRegistry.getDimensions().stream()
                    .sorted()
                    .<Set<Dimension.Partition>>map(d -> new HashSet<>(d.getPartitions()))
                    .toList();
        }

        final var result = new ArrayList<Set<Dimension.Partition>>();
        var cleanInput = input.trim();
        while (cleanInput.startsWith("{") && cleanInput.endsWith("}")) {
            cleanInput = cleanInput.substring(1, cleanInput.length() - 1).trim();
        }

        final var segments = cleanInput.split(";(?![^({]*[)}])");

        for (final var segment : segments) {
            final var matcher = SEGMENT_PATTERN.matcher(segment.trim());
            if (matcher.matches()) {
                final var dimensionName = matcher.group(1).trim();
                final var partitionGroup = matcher.group(2);
                final var options = new HashSet<Dimension.Partition>();

                if (partitionGroup != null) {
                    for (final var partitionName : partitionGroup.split(";")) {
                        options.add(dimensionRegistry.getDimension(dimensionName).getPartition(partitionName.trim()));
                    }
                } else {
                    options.addAll(dimensionRegistry.getDimension(dimensionName).getPartitions());
                }
                result.add(options);
            }
        }
        return result;
    }
}
