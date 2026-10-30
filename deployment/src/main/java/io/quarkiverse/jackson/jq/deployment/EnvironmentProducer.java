package io.quarkiverse.jackson.jq.deployment;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import tools.jackson.databind.JsonNode;

public class EnvironmentProducer {

    public Environment<JsonNode> produceEnvironment() {

        return EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Version.valueOf("1.8.0")).build();
    }
}
