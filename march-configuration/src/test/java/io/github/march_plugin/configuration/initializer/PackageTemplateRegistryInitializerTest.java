package io.github.march_plugin.configuration.initializer;

import io.github.march_plugin.configuration.dto.package_templates.JPackageDto;
import io.github.march_plugin.configuration.dto.package_templates.PackageTemplateDto;
import io.github.march_plugin.configuration.dto.package_templates.PackageTemplatesDto;
import io.github.march_plugin.core.config.package_templates.exception.DuplicationPackageTemplateDefinitionException;
import io.github.march_plugin.core.config.package_templates.exception.PackageTemplateNotFoundException;
import io.github.march_plugin.core.config.projectstructure.exception.EmptyPackageNameException;
import io.github.march_plugin.core.config.projectstructure.exception.IllegalPackageNameException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackageTemplateRegistryInitializerTest {

    private final PackageTemplateRegistryInitializer initializer = new PackageTemplateRegistryInitializer();

    private static PackageTemplatesDto templatesDto(final List<PackageTemplateDto> templates) {
        return new PackageTemplatesDto(templates);
    }

    private static PackageTemplateDto templateDto(final String name, final List<JPackageDto> packages) {
        return new PackageTemplateDto(name, packages);
    }

    private static JPackageDto jpackageDto(final String name, final String partition, final Boolean optional, final List<JPackageDto> children) {
        return new JPackageDto(name, partition, optional, children);
    }

    @Nested
    class EmptyConfiguration {

        @Test
        void shouldBuildEmptyRegistryWhenTemplateListIsNull() {
            final var registry = initializer.build(templatesDto(null));

            assertThatThrownBy(() -> registry.getPackageTemplate("standard"))
                    .isInstanceOf(PackageTemplateNotFoundException.class);
        }

        @Test
        void shouldBuildEmptyRegistryWhenTemplateListIsEmpty() {
            final var registry = initializer.build(templatesDto(List.of()));

            assertThatThrownBy(() -> registry.getPackageTemplate("standard"))
                    .isInstanceOf(PackageTemplateNotFoundException.class);
        }
    }

    @Nested
    class RootPackageBuilding {

        @Test
        void shouldBuildTemplateWithNoPackagesWhenJPackageListIsNull() {
            final var registry = initializer.build(templatesDto(List.of(templateDto("empty-template", null))));

            final var root = registry.getPackageTemplate("empty-template");
            assertThat(root.packageHierarchy()).isNull();
            assertThat(root.partition()).isNull();
            assertThat(root.optional()).isFalse();
            assertThat(root.children()).isEmpty();
        }

        @Test
        void shouldBuildTemplateWithNoPackagesWhenJPackageListIsEmpty() {
            final var registry = initializer.build(templatesDto(List.of(templateDto("empty-template", List.of()))));

            assertThat(registry.getPackageTemplate("empty-template").children()).isEmpty();
        }
    }

    @Nested
    class TopLevelPackageBuilding {

        @Test
        void shouldBuildSingleTopLevelPackage() {
            final var jpackage = jpackageDto("main", "service", true, null);
            final var registry = initializer.build(templatesDto(List.of(templateDto("standard", List.of(jpackage)))));

            final var root = registry.getPackageTemplate("standard");
            assertThat(root.children()).hasSize(1);

            final var child = root.children().getFirst();
            assertThat(child.packageHierarchy().toString()).isEqualTo("main");
            assertThat(child.partition()).isEqualTo("service");
            assertThat(child.optional()).isTrue();
            assertThat(child.children()).isEmpty();
        }

        @Test
        void shouldDefaultOptionalToFalseWhenNotSpecified() {
            final var jpackage = jpackageDto("main", "service", null, null);
            final var registry = initializer.build(templatesDto(List.of(templateDto("standard", List.of(jpackage)))));

            assertThat(registry.getPackageTemplate("standard").children().getFirst().optional()).isFalse();
        }

        @Test
        void shouldRespectExplicitOptionalFalse() {
            final var jpackage = jpackageDto("main", "service", false, null);
            final var registry = initializer.build(templatesDto(List.of(templateDto("standard", List.of(jpackage)))));

            assertThat(registry.getPackageTemplate("standard").children().getFirst().optional()).isFalse();
        }

        @Test
        void shouldAllowPackageWithoutPartition() {
            final var jpackage = jpackageDto("main", null, null, null);
            final var registry = initializer.build(templatesDto(List.of(templateDto("standard", List.of(jpackage)))));

            assertThat(registry.getPackageTemplate("standard").children().getFirst().partition()).isNull();
        }

        @Test
        void shouldBuildMultipleTopLevelPackages() {
            final var main = jpackageDto("main", null, null, null);
            final var test = jpackageDto("test", null, null, null);
            final var registry = initializer.build(templatesDto(List.of(templateDto("standard", List.of(main, test)))));

            final var children = registry.getPackageTemplate("standard").children();
            assertThat(children).hasSize(2);
            assertThat(children.stream().map(c -> c.packageHierarchy().toString())).containsExactlyInAnyOrder("main", "test");
        }
    }

    @Nested
    class NestedPackageBuilding {

        @Test
        void shouldBuildNestedPackageHierarchy() {
            final var sub = jpackageDto("sub", "core", true, null);
            final var main = jpackageDto("main", null, null, List.of(sub));
            final var registry = initializer.build(templatesDto(List.of(templateDto("standard", List.of(main)))));

            final var builtMain = registry.getPackageTemplate("standard").children().getFirst();
            assertThat(builtMain.children()).hasSize(1);

            final var builtSub = builtMain.children().getFirst();
            assertThat(builtSub.packageHierarchy().toString()).isEqualTo("main.sub");
            assertThat(builtSub.packageHierarchy().depth()).isEqualTo(2);
            assertThat(builtSub.partition()).isEqualTo("core");
            assertThat(builtSub.optional()).isTrue();
        }
    }

    @Nested
    class MultipleTemplates {

        @Test
        void shouldBuildMultipleTemplatesIndependently() {
            final var standard = templateDto("standard", List.of(jpackageDto("main", null, null, null)));
            final var minimal = templateDto("minimal", null);
            final var registry = initializer.build(templatesDto(List.of(standard, minimal)));

            assertThat(registry.getPackageTemplate("standard").children()).hasSize(1);
            assertThat(registry.getPackageTemplate("minimal").children()).isEmpty();
        }

        @Test
        void shouldThrowWhenTemplateNameIsDuplicated() {
            final var first = templateDto("standard", null);
            final var second = templateDto("standard", List.of(jpackageDto("main", null, null, null)));

            assertThatThrownBy(() -> initializer.build(templatesDto(List.of(first, second))))
                    .isInstanceOf(DuplicationPackageTemplateDefinitionException.class);
        }
    }

    @Nested
    class InvalidPackageNames {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldThrowWhenTopLevelPackageNameIsBlank(final String invalidName) {
            final var jpackage = jpackageDto(invalidName, null, null, null);
            final var dto = templatesDto(List.of(templateDto("standard", List.of(jpackage))));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(EmptyPackageNameException.class);
        }

        @Test
        void shouldThrowWhenNestedPackageNameIsBlank() {
            final var sub = jpackageDto("", null, null, null);
            final var main = jpackageDto("main", null, null, List.of(sub));
            final var dto = templatesDto(List.of(templateDto("standard", List.of(main))));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(EmptyPackageNameException.class);
        }

        @Test
        void shouldThrowWhenPackageNameContainsDot() {
            final var jpackage = jpackageDto("com.example", null, null, null);
            final var dto = templatesDto(List.of(templateDto("standard", List.of(jpackage))));

            assertThatThrownBy(() -> initializer.build(dto))
                    .isInstanceOf(IllegalPackageNameException.class);
        }
    }
}
