package io.quarkiverse.jackson.jq;

import java.util.Set;

@FunctionalInterface
public interface FunctionFilter {

    boolean accept(FunctionContext context);

    static FunctionFilter denyNames(Set<String> names) {
        Set<String> denied = Set.copyOf(names);
        return context -> !denied.contains(context.getSignature().name());
    }
}
