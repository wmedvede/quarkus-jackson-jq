package io.quarkiverse.jackson.jq.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.jackson.jq")
public interface JacksonJqConfig {

    /**
     * Configure function related config.
     */
    FunctionsConfig functions();

    Map<String, JacksonJqModuleConfig> modules();

    @ConfigGroup
    interface FunctionsConfig {
        /**
         * The supported jq version.
         */
        @WithDefault("1.8.2")
        @WithConverter(JacksonJqVersionConverter.class)
        Version version();

        /**
         * List of functions to exclude.
         */
        Optional<List<String>> excludes();
    }

    @ConfigGroup
    interface JacksonJqModuleConfig {

        /**
         * Enables/disables a particular module. A jackson-jq module included as project dependency is enabled by default.
         *
         * @return true if the module is enabled, false in any other case.
         */
        @WithDefault("true")
        boolean enabled();

    }
}
