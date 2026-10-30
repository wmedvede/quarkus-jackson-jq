package io.quarkiverse.jackson.jq;

import java.util.HashSet;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.function.loaders.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import tools.jackson.databind.JsonNode;

//@Recorder
public class JacksonJqEnvironmentRecorder {

    public Environment<JsonNode> createEnvironment(String version, List<String> excludes,
            List<JacksonJqModuleRegistry.ModuleDescriptor> enabledModules) {
        //        List<JacksonJqModuleRegistry.ModuleDescriptor> enabledModules = new ArrayList<>();

        FunctionFilter filter = FunctionFilter
                .denyNames(new HashSet<>(excludes));
        FilteringModuleLoader<JsonNode> moduleLoader = new FilteringModuleLoader<>(filter);
        enabledModules.forEach(enabledModule -> {
            moduleLoader.registerModule(instantiateModule(enabledModule));

        });

        Environment<JsonNode> environment = EnvironmentBuilder
                .withDefaultLoaders(Jackson3JsonProvider.getInstance(), Version.valueOf(version))
                .clearFunctionLoaders()
                .clearModuleLoaders()
                .addFunctionLoader(new FilteringFunctionLoader(BuiltinFunctionLoader.getInstance(), filter))
                .addModuleLoader(moduleLoader)
                //SE VA, por ahora leemos las funciones del projeto actual
                .addFunctionLoader(
                        new FilteringFunctionLoader(new ClassPathFunctionLoader(Thread.currentThread().getContextClassLoader()),
                                FunctionFilter
                                        .denyNames(new HashSet<>(excludes))))
                .build();
        return environment;
    }

    private static Module instantiateModule(JacksonJqModuleRegistry.ModuleDescriptor descriptor) {
        try {
            Class<?> clazz = Class.forName(
                    descriptor.className(),
                    true,
                    Thread.currentThread().getContextClassLoader());
            return (Module) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to instantiate jackson-jq module: " + descriptor.className(), e);
        }
    }
}
