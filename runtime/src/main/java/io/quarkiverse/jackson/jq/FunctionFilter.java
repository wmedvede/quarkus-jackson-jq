package io.quarkiverse.jackson.jq;

import java.util.Set;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

@FunctionalInterface
public interface FunctionFilter {

    boolean accept(FunctionContext context);

    static FunctionFilter denySignatures(Set<FunctionSignature> signatures) {
        Set<FunctionSignature> denied = Set.copyOf(signatures);
        return context -> !denied.contains(context.getSignature());
    }

    static FunctionFilter denyNames(Set<String> names) {
        Set<String> denied = Set.copyOf(names);
        return context -> !denied.contains(context.getSignature().name());
    }

    static FunctionFilter acceptAll() {
        return AcceptAll.INSTANCE;
    }
}
