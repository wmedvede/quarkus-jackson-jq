package io.quarkiverse.jackson.jq;

import java.util.function.Predicate;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class FilteredModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

    private final ModuleLoader<JsonNode> delegate;
    private final Predicate<FunctionSignature> filter;

    public FilteredModuleLoader(ModuleLoader<JsonNode> delegate, Predicate<FunctionSignature> filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        Module module = delegate.loadModule(path, metadata);

        if (module instanceof JavaModule) {
            // Nice here we can filter.
            ((JavaModule)module).getFunctions();
        }
        if (module instanceof JqModule) {
            // Module was not materialized/compiled yet.
            //We can only get the source code
            ((JqModule)module).getSourceCode();

        }
        return null;
    }

    @Override
    public JsonNode loadData(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        return null;
    }
}
