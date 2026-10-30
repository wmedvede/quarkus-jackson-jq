package io.quarkiverse.jackson.jq;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

public final class FilteringJavaModule implements JavaModule {
    private final JavaModule delegate;
    private final Map<FunctionSignature, Function> functions;

    private FilteringJavaModule(JavaModule delegate, Map<FunctionSignature, Function> functions) {
        this.delegate = delegate;
        this.functions = functions;
    }

    public static JavaModule of(JavaModule module, FunctionFilter filter, String locator) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(locator, "locator");
        if (filter == FunctionFilter.acceptAll())
            return module;
        Map<FunctionSignature, Function> accepted = new LinkedHashMap<>();
        for (Map.Entry<FunctionSignature, Function> entry : module.getFunctions().entrySet()) {
            if (filter.accept(FunctionContext.of(entry.getKey(), locator)))
                accepted.put(entry.getKey(), entry.getValue());
        }
        return new FilteringJavaModule(module, Collections.unmodifiableMap(accepted));
    }

    @Override
    public Map<FunctionSignature, Function> getFunctions() {
        return functions;
    }

    @Override
    public ModuleMeta getModuleMeta() {
        return delegate.getModuleMeta();
    }
}