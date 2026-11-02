package io.quarkiverse.jackson.jq;

import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.Environment;
import tools.jackson.databind.JsonNode;

public class EnvironmentSupplier implements Supplier<Environment<JsonNode>> {

    Environment<JsonNode> environment;

    public EnvironmentSupplier(Environment<JsonNode> environment) {
        this.environment = environment;
    }

    @Override
    public Environment<JsonNode> get() {
        return environment;
    }
}
