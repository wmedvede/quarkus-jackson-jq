package io.quarkiverse.jackson.jq;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import net.thisptr.jackson.jq.v2.core.Environment;
import tools.jackson.databind.JsonNode;

@Singleton
public class EnvironmentProducer {

    private final EnvironmentSupplier environmentSupplier;

    public EnvironmentProducer(EnvironmentSupplier environmentSupplier) {
        this.environmentSupplier = environmentSupplier;
    }

    @Produces
    public Environment<JsonNode> produce() {
        return environmentSupplier.get();
    }
}
