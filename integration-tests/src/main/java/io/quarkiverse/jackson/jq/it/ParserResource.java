/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.jackson.jq.it;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jspecify.annotations.NonNull;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

@Path("/jq")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ParserResource {

    @Inject
    Environment<JsonNode> environment;
    @Inject
    ObjectMapper mapper;

    @Path("/parse")
    @POST
    public List<JsonNode> parse(final Document document) throws JsonQueryException {
        final JsonQuery<JsonNode> query = environment.compile(document.getExpression());
        List<JsonNode> out = new ArrayList<>();
        query.apply(document.getDocument(), out::add);
        return out;
    }

    @Path("/jqfunctions")
    @GET
    public ArrayNode jqFunctions() {
        List<String> functions = collectJqFunctions();
        System.out.println("jqfunctions = " + functions);
        ArrayNode answer = mapper.createArrayNode();
        functions.forEach(answer::add);
        return answer;
    }

    @Path("/functions")
    @GET
    public ArrayNode functions() {
        List<String> functions = collectFunctions();
        System.out.println("functions = " + functions);
        ArrayNode answer = mapper.createArrayNode();
        functions.forEach(answer::add);
        return answer;
    }

    @Path("/allfunctions")
    @GET
    public ArrayNode allFunctions() {
        List<String> allFunctions = new ArrayList<>(collectJqFunctions());
        allFunctions.addAll(collectFunctions());
        ArrayNode answer = mapper.createArrayNode();
        allFunctions.forEach(answer::add);
        return answer;
    }

    private @NonNull List<String> collectJqFunctions() {
        return environment.getFunctionLoaders().stream()
                .flatMap(functionLoader -> functionLoader.getJqFunctions(environment.getJqVersion()).entrySet().stream())
                .map(entry -> entry.getKey().name() + "/" + entry.getKey().arity())
                .sorted().toList();
    }

    private @NonNull List<String> collectFunctions() {
        List<String> functions = environment.getFunctionLoaders().stream()
                .flatMap(functionLoader -> functionLoader.getFunctions(environment.getJqVersion()).entrySet().stream())
                .map(entry -> entry.getKey().name() + "/" + entry.getKey().arity())
                .sorted().toList();
        return functions;
    }

    /*
     * public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
     * Map<FunctionSignature, Function> result = new HashMap<>();
     * System.out.println("DOINT SERVICE LOADER");
     * for (Function factory : ServiceLoader.load(Function.class, Thread.currentThread().getContextClassLoader())) {
     * System.out.println("Factory- > " + factory.getClass().getName());
     * FunctionRegistration[] regs = factory.getClass().getAnnotationsByType(FunctionRegistration.class);
     * for (FunctionRegistration reg : regs) {
     * VersionRange versionRange = VersionRange.from(reg.version());
     * if (!versionRange.contains(jqVersion))
     * continue;
     *
     * result.put(signatureOf(reg), factory);
     * }
     * }
     *
     * return result;
     * }
     *
     * static FunctionSignature signatureOf(FunctionRegistration reg) {
     * return reg.nargs() < 0 ? FunctionSignature.ofVariadic(reg.name()) : FunctionSignature.of(reg.name(), reg.nargs());
     * }
     */
}
