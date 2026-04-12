package io.github.march_plugin.core.config.projectstructure;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackageHierarchyTest {

    @Test
    void toString_ShouldJoinWithDots() {
        final var hierarchy = new PackageHierarchy(List.of("io", "github", "march"));

        assertThat(hierarchy.toString()).isEqualTo("io.github.march");
    }

    @Test
    void equalsAndHashCode_ShouldBeValueBased() {
        final var h1 = new PackageHierarchy(List.of("a", "b"));
        final var h2 = new PackageHierarchy(List.of("a", "b"));
        final var h3 = new PackageHierarchy(List.of("a", "c"));

        assertThat(h1).isEqualTo(h2);
        assertThat(h1.hashCode()).isEqualTo(h2.hashCode());

        assertThat(h1).isNotEqualTo(h3);
        assertThat(h1).isNotEqualTo("string");
    }
}