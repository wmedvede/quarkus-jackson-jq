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
    private record ModuleEntry(Module module, String locator) {
    }

    private final Map<String, ModuleEntry> pathAndModules = new HashMap<>();
    private final FunctionFilter filter;

    public FilteringModuleLoader(FunctionFilter filter) {
        this.filter = filter;
    }

    public void registerModule(Module module, String locator) {
        for (ModuleRegistration annotation : module.getClass().getAnnotationsByType(ModuleRegistration.class)) {
            pathAndModules.put(annotation.path(), new ModuleEntry(module, locator));
        }
    }

    @Override
    public Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        ModuleEntry entry = pathAndModules.get(path);
        if (entry == null) {
            throw new ModuleNotFoundException(path);
        }
        if (entry.module instanceof JavaModule) {
            return FilteringJavaModule.of((JavaModule) entry.module, filter, entry.locator);
        }
        return entry.module;
    }

    @Override
    public JsonNode loadData(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        return null;
    }
}
