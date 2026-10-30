package io.quarkiverse.jackson.jq.deployment;

import java.util.List;

import io.quarkiverse.jackson.jq.JacksonJqModuleRegistry;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The official jackson-jq extension modules whose Maven artifact is present in the current application.
 */
public final class ActiveJacksonJqModulesBuildItem extends SimpleBuildItem {

    private final List<JacksonJqModuleRegistry.ModuleDescriptor> modules;

    public ActiveJacksonJqModulesBuildItem(List<JacksonJqModuleRegistry.ModuleDescriptor> modules) {
        this.modules = List.copyOf(modules);
    }

    public List<JacksonJqModuleRegistry.ModuleDescriptor> getModules() {
        return modules;
    }
}
