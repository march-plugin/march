package io.github.march_plugin.core.config.testutil;

import io.github.march_plugin.core.config.projectstructure.model.ModuleConvention;
import io.github.march_plugin.core.config.projectstructure.model.ModuleModularity;
import io.github.march_plugin.core.config.projectstructure.model.PackageConvention;
import io.github.march_plugin.core.config.projectstructure.model.PackageModularity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public final class MockUtil {

    public static PackageModularity mockPackageModularity() {
        final var packageModularity = mock(PackageModularity.class);
        when(packageModularity.getConvention()).thenReturn(mock(PackageConvention.class));
        return packageModularity;
    }

    public static ModuleModularity mockModuleModularity() {
        final var moduleModularity = mock(ModuleModularity.class);
        when(moduleModularity.getConvention()).thenReturn(mock(ModuleConvention.class));
        return moduleModularity;
    }
}
