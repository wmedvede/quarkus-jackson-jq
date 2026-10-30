package io.quarkiverse.jackson.jq.deployment;

import io.quarkiverse.jackson.jq.FunctionDescriptor;
import io.quarkus.builder.item.MultiBuildItem;

public final class JacksonJqFunctionBuildItem extends MultiBuildItem {
    private final FunctionDescriptor functionDescriptor;

    public JacksonJqFunctionBuildItem(FunctionDescriptor functionDescriptor) {
        this.functionDescriptor = functionDescriptor;
    }

    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }
}
