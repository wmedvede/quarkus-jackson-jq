package io.quarkiverse.jackson.jq;

import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class FilteringModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

    private final Map<String, Module> pathAndModules = new HashMap<>();
    private final FunctionFilter filter;

    public FilteringModuleLoader(FunctionFilter filter) {
        this.filter = filter;
    }

    public void registerModule(Module module) {
        for (ModuleRegistration annotation : module.getClass().getAnnotationsByType(ModuleRegistration.class)) {
            pathAndModules.put(annotation.path(), module);
        }
    }

    @Override
    public Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        Module module = pathAndModules.get(path);
        if (module == null) {
            throw new ModuleNotFoundException(path);
        }
        if (module instanceof JavaModule) {
            return FilteringJavaModule.of((JavaModule) module, filter, FunctionOrigin.ENV_IMPORT, path);
        }
        return module;
        /*
         * if (module instanceof JqModule) {
         * // Module was not materialized/compiled yet.
         * // We can only get the source code
         * return (JqModule) module;
         * }
         */
    }

    @Override
    public JsonNode loadData(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        return null;
    }
}
