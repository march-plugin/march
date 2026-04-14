package io.github.march_plugin.core.config.projectstructure;

import io.github.march_plugin.core.config.projectstructure.exception.EmptyPackageNameException;
import io.github.march_plugin.core.config.projectstructure.exception.IllegalPackageNameException;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackageHierarchyTest {

    @Nested
    class Creation {

        @Test
        void shouldThrowWhenListIsNull() {
            assertThatThrownBy(() -> new PackageHierarchy(null))
                    .isInstanceOf(EmptyPackageNameException.class);
        }

        @Test
        void shouldThrowWhenListIsEmpty() {
            assertThatThrownBy(() -> new PackageHierarchy(List.of()))
                    .isInstanceOf(EmptyPackageNameException.class);
        }

        @Test
        void shouldThrowWhenEntryIsBlank() {
            assertThatThrownBy(() -> new PackageHierarchy(List.of("x", "  ", "y")))
                    .isInstanceOf(EmptyPackageNameException.class);
        }

        @Test
        void shouldThrowWhenEntryIsNull() {
            final var strings = new ArrayList<String>();
            strings.add("x");
            strings.add(null);
            assertThatThrownBy(() -> {
                new PackageHierarchy(strings);
            })
                    .isInstanceOf(EmptyPackageNameException.class);
        }

        @Test
        void shouldThrowWhenAnyElementContainsADot() {
            final var invalidHierarchy = List.of("io", "github.march");

            assertThatThrownBy(() -> new PackageHierarchy(invalidHierarchy))
                    .isInstanceOf(IllegalPackageNameException.class);
        }

        @Test
        void shouldReplaceHyphensWithUnderscores() {
            final var hierarchy = new PackageHierarchy(List.of("my-org", "core-module"));

            assertThat(hierarchy.get(0)).isEqualTo("my_org");
            assertThat(hierarchy.get(1)).isEqualTo("core_module");
            assertThat(hierarchy.toString()).isEqualTo("my_org.core_module");
        }
    }

    @Nested
    class Properties {

        @Test
        void depth_ShouldReturnSizeOfHierarchy() {
            final var hierarchy = new PackageHierarchy(List.of("io", "github", "march"));
            assertThat(hierarchy.depth()).isEqualTo(3);
        }

        @Test
        void get_ShouldReturnElementAtGivenIndex() {
            final var hierarchy = new PackageHierarchy(List.of("io", "github", "march"));

            assertThat(hierarchy.get(0)).isEqualTo("io");
            assertThat(hierarchy.get(1)).isEqualTo("github");
            assertThat(hierarchy.get(2)).isEqualTo("march");
        }

        @Test
        void getSimpleName_ShouldReturnLastElement() {
            final var hierarchy = new PackageHierarchy(List.of("io", "github", "march"));
            assertThat(hierarchy.getSimpleName()).isEqualTo("march");
        }

        @Test
        void toString_ShouldJoinWithDots() {
            final var hierarchy = new PackageHierarchy(List.of("io", "github", "march"));
            assertThat(hierarchy.toString()).isEqualTo("io.github.march");
        }
    }

    @Nested
    class PathBuilding {

        @Test
        void buildPath_ShouldResolveCorrectlyFromRoot() {
            final var hierarchy = new PackageHierarchy(List.of("io", "github", "march"));
            final var rootPath = "src/main/java";

            final var path = hierarchy.buildPath(rootPath);

            final var expectedPath = Paths.get(rootPath, "io", "github", "march");
            assertThat(path).isEqualTo(expectedPath);
        }
    }

    @Nested
    class ChildBuilding {

        @Test
        void buildChild_WithNullParent_ShouldCreateNewHierarchy() {
            final var child = PackageHierarchy.buildChild(null, "march");

            assertThat(child.depth()).isEqualTo(1);
            assertThat(child.toString()).isEqualTo("march");
        }

        @Test
        void buildChild_WithNonNullParent_ShouldAppendToParent() {
            final var parent = new PackageHierarchy(List.of("io", "github"));
            final var child = PackageHierarchy.buildChild(parent, "march");

            assertThat(child.depth()).isEqualTo(3);
            assertThat(child.toString()).isEqualTo("io.github.march");
        }

        @Test
        void buildChild_ShouldApplyConstructorTransformationsToChild() {
            final var parent = new PackageHierarchy(List.of("io"));
            final var child = PackageHierarchy.buildChild(parent, "my-plugin");

            assertThat(child.getSimpleName()).isEqualTo("my_plugin");
            assertThat(child.toString()).isEqualTo("io.my_plugin");
        }

        @Test
        void buildChild_ShouldApplyConstructorValidationsToChild() {
            final var parent = new PackageHierarchy(List.of("io"));

            assertThatThrownBy(() -> PackageHierarchy.buildChild(parent, "invalid.pkg"))
                    .isInstanceOf(IllegalPackageNameException.class);
        }
    }

    @Nested
    class EqualityAndHashCode {

        @Test
        void equalsAndHashCode_ShouldBeValueBased() {
            final var h1 = new PackageHierarchy(List.of("a", "b"));
            final var h2 = new PackageHierarchy(List.of("a", "b"));
            final var h3 = new PackageHierarchy(List.of("a", "c"));

            assertThat(h1).isEqualTo(h2);
            assertThat(h1.hashCode()).isEqualTo(h2.hashCode());

            assertThat(h1).isNotEqualTo(h3);
            assertThat(h1).isNotEqualTo("string");
            assertThat(h1).isNotEqualTo(null);
        }
    }
}