package io.quarkiverse.jackson.jq;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

public class ExcludeFunctionSignatureFilter implements Predicate<FunctionSignature> {

    private final Set<String> excludes;

    private ExcludeFunctionSignatureFilter(Set<String> excludes) {
        this.excludes = excludes;
    }

    @Override
    public boolean test(FunctionSignature functionSignature) {
        return !excludes.contains(functionSignature.name());
    }

    public static ExcludeFunctionSignatureFilter of(List<String> excludes) {
        HashSet<String> exclusionSet = new HashSet<>(excludes != null ? excludes : new ArrayList<>());
        return new ExcludeFunctionSignatureFilter(exclusionSet);
    }
}
