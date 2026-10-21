package io.quarkiverse.jackson.jq;

import java.util.function.Predicate;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
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

        return null;
    }

    @Override
    public JsonNode loadData(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
        return null;
    }
}
