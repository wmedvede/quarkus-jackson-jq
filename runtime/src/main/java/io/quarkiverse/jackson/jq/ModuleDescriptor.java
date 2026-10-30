package io.quarkiverse.jackson.jq;

public record ModuleDescriptor(String id, String groupId, String artifactId, String className, boolean bundled) {
}