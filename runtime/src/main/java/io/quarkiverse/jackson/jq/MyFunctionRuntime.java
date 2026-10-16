package io.quarkiverse.jackson.jq;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@FunctionRegistration(name = "myFunctionRuntime", nargs = 2)
public class MyFunctionRuntime implements Function {
    @Override
    public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx,
            List<Expression<Context, JsonNode>> args) throws JsonQueryException {
        throw new JsonQueryException("Not Yet Implemented");
    }

    /*
     * @JqFunction({ "myFunction/1", "myFunction/2" })
     * public class MyFunction implements Function {
     *
     * @Override
     * public void apply(Scope scope, List<Expression> args, JsonNode in, Path path, PathOutput output, Version version)
     * throws JsonQueryException {
     * throw new JsonQueryException("Not Yet Implemented");
     * }
     *
     */
}
