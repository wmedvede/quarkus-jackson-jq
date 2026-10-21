package io.quarkiverse.jackson.jq;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class FilteredFunctionLoader implements FunctionLoader {

    private final FunctionLoader delegate;
    private final Predicate<FunctionSignature> filter;

    public FilteredFunctionLoader(FunctionLoader delegate, Predicate<FunctionSignature> filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
        return delegate.getFunctions(jqVersion).entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion) {
        return delegate.getJqFunctions(jqVersion).entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
