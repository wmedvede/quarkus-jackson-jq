package io.quarkiverse.jackson.jq.deployment;

import io.quarkiverse.jackson.jq.ModuleDescriptor;
import io.quarkus.builder.item.MultiBuildItem;

public final class JacksonJqModuleBuildItem extends MultiBuildItem {
    private final ModuleDescriptor moduleDescriptor;

    public JacksonJqModuleBuildItem(ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    public ModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }
}
