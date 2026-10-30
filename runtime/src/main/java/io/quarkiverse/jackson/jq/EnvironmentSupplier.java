package io.quarkiverse.jackson.jq;

import net.thisptr.jackson.jq.v2.core.Environment;
import tools.jackson.databind.JsonNode;

public class EnvironmentSupplier {

    Environment<JsonNode> env;

    public EnvironmentSupplier(Environment<JsonNode> env) {
        this.env = env;
    }

    public Environment<JsonNode> get() {
        return env;
    }
}
