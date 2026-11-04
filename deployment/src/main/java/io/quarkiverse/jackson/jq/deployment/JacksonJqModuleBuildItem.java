package io.quarkiverse.jackson.jq.deployment;

import java.util.Objects;

import io.quarkiverse.jackson.jq.ModuleDescriptor;
import io.quarkus.builder.item.MultiBuildItem;

public final class JacksonJqModuleBuildItem extends MultiBuildItem {
    private final ModuleDescriptor moduleDescriptor;
    private final JacksonJqConfig.Strategy strategy;
    private final String path;
    private final String alias;

    private JacksonJqModuleBuildItem(ModuleDescriptor moduleDescriptor,
            JacksonJqConfig.Strategy strategy, String path, String alias) {
        this.moduleDescriptor = moduleDescriptor;
        this.strategy = strategy;
        this.path = path;
        this.alias = alias;
    }

    public static JacksonJqModuleBuildItem forRegister(ModuleDescriptor moduleDescriptor, String path) {
        return new JacksonJqModuleBuildItem(moduleDescriptor, JacksonJqConfig.Strategy.REGISTER, path, null);
    }

    public static JacksonJqModuleBuildItem forImport(ModuleDescriptor moduleDescriptor, String alias) {
        return new JacksonJqModuleBuildItem(moduleDescriptor, JacksonJqConfig.Strategy.IMPORT, null,
                Objects.requireNonNull(alias, "An alias must be provided."));
    }

    public static JacksonJqModuleBuildItem forInclude(ModuleDescriptor moduleDescriptor) {
        return new JacksonJqModuleBuildItem(moduleDescriptor, JacksonJqConfig.Strategy.INCLUDE, null, null);
    }

    public ModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }

    public JacksonJqConfig.Strategy getStrategy() {
        return strategy;
    }

    public String getPath() {
        return path;
    }

    public String getAlias() {
        return alias;
    }
}
