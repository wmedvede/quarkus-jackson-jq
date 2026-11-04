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

/**
 * A {@link ModuleLoader} that resolves modules by the path of their {@link ModuleRegistration} annotations, and only
 * exposes the functions of the {@link JavaModule}s accepted by a {@link FunctionFilter}.
 *
 * @param <JsonNode> the JSON node type.
 */
public class FilteringModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
    private record ModuleEntry(Module module, String locator) {
    }

    private final Map<String, ModuleEntry> moduleByPath = new HashMap<>();
    private final FunctionFilter filter;

    /**
     * @param filter the filter that decides which functions of the Java modules are exposed.
     */
    public FilteringModuleLoader(FunctionFilter filter) {
        this.filter = filter;
    }

    /**
     * Makes the module available under each path declared by its {@link ModuleRegistration} annotations.
     *
     * @param module the module to register.
     * @param locator the {@link FunctionContext#getLocator() locator} given to the filter, the module id.
     */
    public void registerModule(Module module, String locator) {
        for (ModuleRegistration annotation : module.getClass().getAnnotationsByType(ModuleRegistration.class)) {
            moduleByPath.put(annotation.path(), new ModuleEntry(module, locator));
        }
    }

    @Override
    public Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        ModuleEntry entry = moduleByPath.get(path);
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
