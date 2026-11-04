package io.quarkiverse.jackson.jq;

import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

public class FilteringJavaModule implements JavaModule {
    private final JavaModule delegate;
    private final Map<FunctionSignature, Function> functions;

    protected FilteringJavaModule(JavaModule module, FunctionFilter filter, String locator) {
        this.delegate = module;
        this.functions = new HashMap<>();
        for (Map.Entry<FunctionSignature, Function> entry : module.getFunctions().entrySet()) {
            if (filter.accept(FunctionContext.of(entry.getKey(), locator)))
                this.functions.put(entry.getKey(), entry.getValue());
        }
    }

    public static JavaModule of(JavaModule module, FunctionFilter filter, String locator) {
        return new FilteringJavaModule(module, filter, locator);
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
