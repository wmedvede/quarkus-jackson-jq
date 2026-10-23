package io.quarkiverse.jackson.jq;

import java.util.HashSet;
import java.util.List;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.function.FunctionFilter;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import tools.jackson.databind.JsonNode;

@Recorder
public class JacksonJqEnvironmentRecorder {

    public RuntimeValue<Environment<JsonNode>> createEnvironment(String version, List<String> exclusions) {
        Environment<JsonNode> environment = EnvironmentBuilder
                .withDefaultLoaders(Jackson3JsonProvider.getInstance(), Version.valueOf(version))
                .setFunctionFilter(new MyFunctionFilter())
//                .setFunctionFilter(FunctionFilter.denyNames(new HashSet<>(exclusions)))
                //                .registerModule("my-path-jackson-jq/uuid", new UuidModule())
                .addFunctionLoader(new ClassPathFunctionLoader(Thread.currentThread().getContextClassLoader()))
                .addModuleLoader(new ClassPathModuleLoader<>(Thread.currentThread().getContextClassLoader()))
                .build();
        return new RuntimeValue<>(environment);
    }
}
