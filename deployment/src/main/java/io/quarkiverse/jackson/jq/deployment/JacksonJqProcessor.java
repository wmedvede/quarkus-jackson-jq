package io.quarkiverse.jackson.jq.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkiverse.jackson.jq.JacksonJqScopeRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistrations;
import tools.jackson.databind.JsonNode;

class JacksonJqProcessor {
    private static final String FEATURE = "jackson-jq";
    private static final String JACKSON_JQ_GROUP_ID = "net.thisptr.jackson.jq.v2";
    private static final String JACKSON_JQ_ARTIFACT_ID = "jackson-jq-core";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Ensure Quarkus indexes net.thisptr.jackson.jq.v2:jackson-jq-core at build time.
     * This avoids “Reindexing … index version is 10” warnings.
     */
    @BuildStep
    IndexDependencyBuildItem indexJacksonJq() {
        return new IndexDependencyBuildItem(JACKSON_JQ_GROUP_ID, JACKSON_JQ_ARTIFACT_ID);
    }

    /**
     * Likewise, index jackson-jq-extra.
     */
    /*
     * @BuildStep
     * IndexDependencyBuildItem indexJacksonJqExtra() {
     * return new IndexDependencyBuildItem(JACKSON_JQ_GROUP_ID, JACKSON_JQ_EXTRA_ARTIFACT_ID);
     * }
     *
     */

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem quarkusEnvironmentBean(
            CombinedIndexBuildItem combinedIndex,
            ApplicationArchivesBuildItem archives,
            JacksonJqConfig config,
            JacksonJqScopeRecorder recorder,
            List<JacksonJqFunctionBuildItem> functions,
            RecorderContext context) throws Exception {

        System.out.println("XXXXXXXXXX quarkusScopeBean() build step");

        IndexView indexView = combinedIndex.getIndex();
        List<String> excludes = config.functions().excludes().orElseGet(Collections::emptyList);

        RuntimeValue<Environment<JsonNode>> environment = recorder.createEnvironment(config.functions().version().toString());
        /*
         * // load custom function
         * lookupFunctions(indexView, config, context, JqFunction.class).forEach(f -> {
         * if (!excludes.contains(StringUtils.substringBefore(f.getName(), '/'))) {
         * recorder.addFunction(local, f.getName(), f.getFunction());
         * }
         * });
         * functions.forEach(f -> {
         * if (!excludes.contains(StringUtils.substringBefore(f.getName(), '/'))) {
         * recorder.addFunction(local, f.getName(), f.getFunction());
         * }
         * });
         */

        DotName envDotName = DotName.createSimple(Environment.class.getName());
        DotName jsonNodeDotName = DotName.createSimple(JsonNode.class.getName());
        Type jsonNodeType = ClassType.create(jsonNodeDotName, Type.Kind.CLASS);
        ParameterizedType envType = ParameterizedType.create(envDotName, new Type[] { jsonNodeType }, null);

        return SyntheticBeanBuildItem
                .configure(envDotName)
                .addType(envType)
                .scope(Singleton.class)
                .setRuntimeInit()
                .runtimeValue(environment)
                .done();
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(Environment.class);
    }

    @BuildStep
    void registerFunctionsForNative(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(Function.class.getName()));
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(JqLibrary.class.getName()));
    }

    /**
     * Registers all {@link Function} implementations for native image support.
     * <p>
     * In native mode {@code ServiceLoader} cannot scan the classpath at runtime, so every
     * provider must be declared up-front. This build step collects every class in the
     * combined Jandex index that implements {@link Function} and is annotated with
     * {@link FunctionRegistration}, then:
     * <ul>
     * <li>produces a {@link ServiceProviderBuildItem} so GraalVM includes them in the
     * native {@code META-INF/services} registry, and</li>
     * <li>produces a {@link ReflectiveClassBuildItem} so they can be instantiated
     * reflectively by {@code ServiceLoader} at runtime.</li>
     * </ul>
     */
    //    @BuildStep
    void registerFunctionsForNative(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        System.out.println("XXXXXXXXXX registerFunctionsForNative");
        IndexView index = combinedIndex.getIndex();
        DotName functionName = DotName.createSimple(Function.class.getName());
        DotName functionRegistrationName = DotName.createSimple(FunctionRegistration.class.getName());
        DotName functionRegistrationsName = DotName.createSimple(FunctionRegistrations.class.getName());
        List<String> implementations = index.getAllKnownImplementors(functionName.toString()).stream()
                .filter(ci -> {

                    System.out.println("filtering: " + ci.name());
                    System.out.println("annotations: " + ci.annotations(functionRegistrationName).size());
                    //                    System.out.println(
                    //                            "annotationsWithRepeatable: " + ci.annotationsWithRepeatable(registrationName, index).size());

                    return !ci.annotations(functionRegistrationName).isEmpty()
                            || !ci.annotations(functionRegistrationsName).isEmpty();

                })
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toList());

        List<String> coreFunctions = implementations.stream().filter(func -> func.startsWith("net.thisptr.jackson.jq.v2.core."))
                .toList();
        implementations.forEach(fundFunction -> {
            System.out.println("function found for native -> : " + fundFunction);
        });
        if (!coreFunctions.isEmpty()) {
            serviceProviders.produce(new ServiceProviderBuildItem(
                    Function.class.getName(),
                    coreFunctions.toArray(new String[0])));
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                    coreFunctions.toArray(new String[0]))
                    .constructors(true)
                    .build());
        }
    }

    //    @BuildStep
    //    void dynamicServiceProviderRegistration(BuildProducer<ServiceProviderBuildItem> serviceProviders,
    //            CombinedIndexBuildItem combinedIndexBuildItem) {
    //
    //        System.out.println("XXXXXXXXXXXXXXXXXXXXX Registering dynamic providers for FunctionSSSSSSSS: ");
    //
    //        IndexView index = combinedIndexBuildItem.getIndex();
    //        DotName targetInterface = DotName.createSimple(Function.class.getName());
    //
    //        // Dynamically find all known implementations/subclasses in the index
    //        List<String> implementations = new ArrayList<>();
    //        for (org.jboss.jandex.ClassInfo classInfo : index.getAllKnownImplementors(targetInterface)) {
    //            // Optional: filter out abstract classes or interfaces if necessary
    //            //            if (!org.jboss.jandex.Modifier.isAbstract(classInfo.flags())) {
    //            //                implementations.add(classInfo.name().toString());
    //            //            }
    //        }
    //
    //        if (!implementations.isEmpty()) {
    //            System.out.println("XXXXXXXXXXXXXXXXXXXXX Registering dynamic providers for Function: " + implementations);
    //
    //            // Produce the build item with the dynamically discovered class names
    //            serviceProviders.produce(new ServiceProviderBuildItem(
    //                    Function.class.getName(),
    //                    implementations.toArray(new String[0])));
    //        }
    //    }

    /**
     * Registers {@link Function} implementations for native image support based on the
     * {@code META-INF/services} files already present in every application archive.
     * <p>
     * This is the source-of-truth approach: only classes that a library explicitly declared
     * in its own service descriptor are registered. This avoids registering classes that
     * happen to implement {@link Function} but were never intended as SPI providers.
     */
    //    @BuildStep
    void registerFunctionsForNativeFromServiceFiles(
            ApplicationArchivesBuildItem archives,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        String serviceFile = "META-INF/services/" + Function.class.getName();
        Set<String> declaredProviders = new LinkedHashSet<>();

        archives.getAllArchives().forEach(archive -> {
            java.nio.file.Path path = archive.getChildPath(serviceFile);
            if (path == null || !java.nio.file.Files.exists(path)) {
                return;
            }
            try (BufferedReader reader = java.nio.file.Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(declaredProviders::add);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + serviceFile + " from archive " + archive.getKey(), e);
            }
        });

        if (!declaredProviders.isEmpty()) {
            String[] providers = declaredProviders.toArray(new String[0]);
            serviceProviders.produce(new ServiceProviderBuildItem(Function.class.getName(), providers));
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(providers).constructors(true).build());
        }
    }
}
