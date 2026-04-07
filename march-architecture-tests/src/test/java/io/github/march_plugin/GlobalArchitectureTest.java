package io.github.march_plugin;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "io.github.march_plugin")
public class GlobalArchitectureTest {

    @ArchTest
    static final ArchRule no_cycles_between_modules =
            slices().matching("io.github.march_plugin.(**)")
                    .should().beFreeOfCycles();
}