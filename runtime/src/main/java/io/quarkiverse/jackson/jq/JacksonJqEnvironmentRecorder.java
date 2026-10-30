package io.quarkiverse.jackson.jq;

import static io.quarkiverse.jackson.jq.Utils.createInstance;

import java.util.HashSet;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.function.loaders.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import tools.jackson.databind.JsonNode;

public class JacksonJqEnvironmentRecorder {

    public Environment<JsonNode> createEnvironment(String version, List<String> excludes,
            List<ModuleDescriptor> enabledModules, List<FunctionDescriptor> customFunctions) {

        FunctionFilter filter = FunctionFilter.denyNames(new HashSet<>(excludes));
        FilteringModuleLoader<JsonNode> moduleLoader = new FilteringModuleLoader<>(filter);
        enabledModules.forEach(enabledModule -> {
            moduleLoader.registerModule(instantiateModule(enabledModule), enabledModule.id());
        });

        return EnvironmentBuilder
                .withDefaultLoaders(Jackson3JsonProvider.getInstance(), Version.valueOf(version))
                .clearFunctionLoaders()
                .clearModuleLoaders()
                .addFunctionLoader(new FilteringFunctionLoader(BuiltinFunctionLoader.getInstance(), filter))
                .addFunctionLoader(
                        new FilteringFunctionLoader(new InMemoryFunctionLoader(customFunctions, Version.valueOf(version)),
                                filter))
                .addModuleLoader(moduleLoader)
                .build();
    }

    private static Module instantiateModule(ModuleDescriptor descriptor) {
        try {
            return createInstance(descriptor.className());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to instantiate jackson-jq module: " + descriptor.className(), e);
        }
    }
}
