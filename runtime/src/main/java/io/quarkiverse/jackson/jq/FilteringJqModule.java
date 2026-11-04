package io.quarkiverse.jackson.jq;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

public class FilteringJqModule implements JqModule {

    private final JqModule delegate;
    private final FunctionFilter filter;
    private final String locator;

    public FilteringJqModule(JqModule delegate, FunctionFilter filter, String locator) {
        this.delegate = delegate;
        this.filter = filter;
        this.locator = locator;
    }

    @Override
    public String getSourceCode() {
        return delegate.getSourceCode();
    }

    @Override
    public Module loadModule(String importPath, @Nullable String searchPath) throws JsonQueryException {
        Module module = delegate.loadModule(importPath, searchPath);
        if (module instanceof JavaModule) {
            return new FilteringJavaModule((JavaModule) module, filter, locator);
        }
        return module;
    }

    @Override
    public <JsonNode> JsonNode loadData(String importPath, @Nullable String searchPath, JsonProvider<JsonNode> jsonProvider)
            throws JsonQueryException {
        return delegate.loadData(importPath, searchPath, jsonProvider);
    }

    @Override
    public ModuleMeta getModuleMeta() {
        return delegate.getModuleMeta();
    }
}
