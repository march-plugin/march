package io.github.march_plugin;

import io.github.march_plugin.configuration.dto.rules.RuleSetDto;
import io.github.march_plugin.core.config.rules.config.DependencyConfig;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarchEquivalenceCheckMojoTest {

    private static final File CONFIG_FILE = new File("march-config.xml");

    private static RuleSetDto ruleSet(final String name) {
        return new RuleSetDto(name, null, null);
    }

    @Nested
    class ResolveDependencyConfig {

        @Test
        void shouldReturnTheSharedValueWhenBothSidesAgree() throws MojoExecutionException {
            final var resolved = MarchEquivalenceCheckMojo.resolveDependencyConfig("a", DependencyConfig.LEAVES_ONLY, "b", DependencyConfig.LEAVES_ONLY);

            assertThat(resolved).isEqualTo(DependencyConfig.LEAVES_ONLY);
        }

        @Test
        void shouldThrowWhenTheTwoRuleSetsDisagree() {
            assertThatThrownBy(() -> MarchEquivalenceCheckMojo.resolveDependencyConfig("a", DependencyConfig.ANY_LEVEL, "b", DependencyConfig.LEAVES_ONLY))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("a").hasMessageContaining("b");
        }
    }

    @Nested
    class ResolvePairs {

        @Test
        void shouldPairTheTwoNamedSetsWhenMarchSetsIsGiven() throws MojoExecutionException {
            final var allow = ruleSet("allow");
            final var deny = ruleSet("deny");

            final var pairs = MarchEquivalenceCheckMojo.resolvePairs("allow;deny", CONFIG_FILE, List.of(allow, deny));

            assertThat(pairs).containsExactly(new MarchEquivalenceCheckMojo.RuleSetPair(allow, deny));
        }

        @Test
        void shouldThrowWhenMarchSetsDoesNotNameExactlyTwoRuleSets() {
            assertThatThrownBy(() -> MarchEquivalenceCheckMojo.resolvePairs("allow", CONFIG_FILE, List.of(ruleSet("allow"))))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("march.sets");
        }

        @Test
        void shouldThrowWhenANamedRuleSetIsNotDeclared() {
            assertThatThrownBy(() -> MarchEquivalenceCheckMojo.resolvePairs("allow;deny", CONFIG_FILE, List.of(ruleSet("allow"))))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("deny");
        }

        @Test
        void shouldThrowWhenFewerThanTwoRuleSetsAreDeclaredAndMarchSetsIsOmitted() {
            assertThatThrownBy(() -> MarchEquivalenceCheckMojo.resolvePairs(null, CONFIG_FILE, List.of(ruleSet("allow"))))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("at least two");
        }

        @Test
        void shouldPairEveryCombinationWhenMarchSetsIsOmitted() throws MojoExecutionException {
            final var a = ruleSet("a");
            final var b = ruleSet("b");
            final var c = ruleSet("c");

            final var pairs = MarchEquivalenceCheckMojo.resolvePairs(null, CONFIG_FILE, List.of(a, b, c));

            assertThat(pairs).containsExactly(
                    new MarchEquivalenceCheckMojo.RuleSetPair(a, b),
                    new MarchEquivalenceCheckMojo.RuleSetPair(a, c),
                    new MarchEquivalenceCheckMojo.RuleSetPair(b, c));
        }
    }
}
