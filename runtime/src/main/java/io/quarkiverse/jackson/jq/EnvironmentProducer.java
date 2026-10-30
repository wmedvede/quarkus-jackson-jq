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

    //TODO find a way to register this Producer bean programatically so we don't have the define a full bean.
    @Produces
    public Environment<JsonNode> produceEnvironment() {
        return environmentSupplier.get();
    }
}
