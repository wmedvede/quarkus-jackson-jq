package io.quarkiverse.jackson.jq;

public record EnabledModule(ModuleDescriptor moduleDescriptor, String filteringModule, String strategy, String alias,
        String path) {

    boolean isRegister() {
        return "REGISTER".equals(strategy);
    }

    boolean isImport() {
        return "IMPORT".equals(strategy);
    }

    boolean isInclude() {
        return "INCLUDE".equals(strategy);
    }
}
