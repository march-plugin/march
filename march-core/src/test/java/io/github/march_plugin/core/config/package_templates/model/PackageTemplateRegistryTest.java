package io.github.march_plugin.core.config.package_templates.model;

import io.github.march_plugin.core.config.package_templates.exception.DuplicationPackageTemplateDefinitionException;
import io.github.march_plugin.core.config.package_templates.exception.PackageTemplateNotFoundException;
import io.github.march_plugin.core.config.projectstructure.model.PackageHierarchy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PackageTemplateRegistryTest {

    private PackageTemplateRegistry.Builder builder;
    private JPackage dummyPackage;

    @BeforeEach
    void setUp() {
        builder = new PackageTemplateRegistry.Builder();
        dummyPackage = new JPackage(mock(PackageHierarchy.class), "main", false, List.of());
    }

    @Test
    void shouldRegisterAndGetTemplate() {
        builder.addPackageTemplate("standard-java", dummyPackage);
        final var registry = builder.build();
        final var result = registry.getPackageTemplate("standard-java");

        assertThat(result).isNotNull();

        assertEquals("main", result.partition());
    }

    @Test
    void shouldThrowExceptionOnDuplicate() {
        builder.addPackageTemplate("duplicate", dummyPackage);

        assertThrows(DuplicationPackageTemplateDefinitionException.class, () -> {
            builder.addPackageTemplate("duplicate", dummyPackage);
        });
    }

    @Test
    void shouldThrowExceptionWhenNotFound() {
        final var registry = builder.build();

        assertThrows(PackageTemplateNotFoundException.class, () -> {
            registry.getPackageTemplate("non-existent");
        });
    }

    @Test
    void shouldBeImmutable() {
        builder.addPackageTemplate("template1", dummyPackage);
        final var registry = builder.build();

        builder.addPackageTemplate("template2", dummyPackage);

        assertThrows(PackageTemplateNotFoundException.class, () -> {
            registry.getPackageTemplate("template2");
        });
    }
}