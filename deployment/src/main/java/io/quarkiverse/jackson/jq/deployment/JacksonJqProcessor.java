package io.quarkiverse.jackson.jq.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkiverse.jackson.jq.EnvironmentSupplier;
import io.quarkiverse.jackson.jq.FunctionDescriptor;
import io.quarkiverse.jackson.jq.JacksonJqConfig;
import io.quarkiverse.jackson.jq.JacksonJqEnvironmentRecorder;
import io.quarkiverse.jackson.jq.JacksonJqModuleRegistry;
import io.quarkiverse.jackson.jq.JacksonJqModuleRegistry.ModuleDescriptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistrations;
import tools.jackson.databind.JsonNode;

class JacksonJqProcessor {
    private static final String FEATURE = "jackson-jq";

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonJqProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    private static List<ModuleDescriptor> activeModules(CurateOutcomeBuildItem curateOutcome) {
        List<ModuleDescriptor> activeModules = new ArrayList<>();
        curateOutcome.getApplicationModel().getRuntimeDependencies()
                .forEach(dep -> {
                    if (JacksonJqModuleRegistry.GROUP_ID.equals(dep.getGroupId())) {
                        ModuleDescriptor moduleDescriptor = JacksonJqModuleRegistry.findByArtifactId(dep.getArtifactId());
                        if (moduleDescriptor != null) {
                            activeModules.add(moduleDescriptor);
                        }
                    }
                });
        return activeModules;
    }

    private static FunctionDescriptor newFunctionDescriptor(String className, AnnotationInstance annotation) {
        String name = annotation.value("name").asString();
        int nargs = annotation.value("nargs").asInt();
        return new FunctionDescriptor(name, nargs, className);
    }

    private static List<FunctionDescriptor> customFunctions(IndexView indexView) {
        DotName functionName = DotName.createSimple(Function.class.getName());
        DotName functionRegistrationName = DotName.createSimple(FunctionRegistration.class.getName());
        DotName functionRegistrationsName = DotName.createSimple(FunctionRegistrations.class.getName());
        List<FunctionDescriptor> functionDescriptors = new ArrayList<>();

        indexView.getAllKnownImplementations(functionName)
                .forEach(functionClass -> {
                    System.out.println("filtering: " + functionClass.name());
                    System.out.println("annotations: " + functionClass.annotations(functionRegistrationName).size());
                    List<AnnotationInstance> functionRegistrations = new ArrayList<>();
                    List<AnnotationInstance> functionRegistrationAnnotations = functionClass
                            .annotations(functionRegistrationName);
                    if (!functionRegistrationAnnotations.isEmpty()) {
                        functionRegistrations.add(functionRegistrationAnnotations.getFirst());
                    } else {
                        List<AnnotationInstance> functionRegistrationsAnnotations = functionClass
                                .annotations(functionRegistrationsName);
                        if (!functionRegistrationsAnnotations.isEmpty()) {
                            AnnotationInstance[] functionRegistrationInstances = functionClass
                                    .annotations(functionRegistrationsName)
                                    .getFirst().values().getFirst().asNestedArray();
                            functionRegistrations.addAll(Arrays.asList(functionRegistrationInstances));
                        }
                    }
                    functionRegistrations.forEach(annotationInstance -> {
                        functionDescriptors.add(newFunctionDescriptor(functionClass.name().toString(), annotationInstance));
                    });
                });
        return functionDescriptors;
    }

    //    @BuildStep
    //    @Record(ExecutionTime.STATIC_INIT)
    /*
     * void quarkusEnvironmentBean(
     * JacksonJqConfig config,
     * JacksonJqEnvironmentRecorder recorder,
     * CurateOutcomeBuildItem curateOutcomeBuildItem,
     * CombinedIndexBuildItem combinedIndex,
     * BuildProducer<SyntheticBeanBuildItem> syntheticBeansBuildItemProducer,
     * BuildProducer<ActiveJacksonJqModulesBuildItem> activeModulesBuildItemBuildProducer) {
     *
     * List<JacksonJqModuleRegistry.ModuleDescriptor> activeModules = activeModules(curateOutcomeBuildItem);
     * Map<String, JacksonJqConfig.JacksonJqModuleConfig> configuredModules = new HashMap<>(config.modules());
     * List<JacksonJqModuleRegistry.ModuleDescriptor> enabledModules = new ArrayList<>();
     * activeModules.forEach(activeModule -> {
     * JacksonJqConfig.JacksonJqModuleConfig moduleConfig = configuredModules.get(activeModule.id());
     * if (moduleConfig == null || moduleConfig.enabled()) {
     * enabledModules.add(activeModule);
     * }
     * configuredModules.remove(activeModule.id());
     * });
     * configuredModules.forEach((id, configuredModule) -> {
     * JacksonJqModuleRegistry.ModuleDescriptor moduleDescriptor = JacksonJqModuleRegistry.findById(id);
     * if (moduleDescriptor == null) {
     * LOGGER.warn("Configured module quarkus.jackson.jq.modules.{} is not recognized as a valid JacksonJq module.",
     * id);
     * } else if (configuredModule.enabled()) {
     * LOGGER.warn("""
     * Configured module quarkus.jackson.jq.modules.{} is enabled but was not added as a project\
     * dependency. You must add the following dependency to your project:\s
     * \
     * <dependency>
     * <groupId>{}</groupId>
     * <artifactId>{}</artifactId>
     * <version>${jackson-jq.version}</version>
     * </dependency>""", id, JacksonJqModuleRegistry.GROUP_ID, moduleDescriptor.artifactId());
     * }
     * });
     * activeModulesBuildItemBuildProducer.produce(new ActiveJacksonJqModulesBuildItem(enabledModules));
     *
     * List<FunctionDescriptor> functions = customFunctions(combinedIndex.getIndex());
     *
     * RuntimeValue<Environment<JsonNode>> environment = recorder.createEnvironment(config.functions().version().toString(),
     * config.functions().excludes().orElse(new ArrayList<>()), enabledModules);
     *
     * DotName envDotName = DotName.createSimple(Environment.class.getName());
     * DotName jsonNodeDotName = DotName.createSimple(JsonNode.class.getName());
     * Type jsonNodeType = ClassType.create(jsonNodeDotName, Type.Kind.CLASS);
     * ParameterizedType envType = ParameterizedType.create(envDotName, new Type[] { jsonNodeType }, null);
     *
     * syntheticBeansBuildItemProducer.produce(SyntheticBeanBuildItem
     * .configure(envDotName)
     * // .setRuntimeInit()
     * .addType(envType)
     * .scope(Singleton.class)
     * .runtimeValue(environment)
     * .done());
     * }
     */

    @BuildStep
    void quarkusEnvironmentBeanBeta2(
            //            JacksonJqConfig config,
            //            JacksonJqEnvironmentRecorder recorder,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeansBuildItemProducer,
            BuildProducer<ActiveJacksonJqModulesBuildItem> activeModulesBuildItemBuildProducer,
            ServiceRegistrar serviceRegistrar) {

        List<JacksonJqModuleRegistry.ModuleDescriptor> activeModules = activeModules(curateOutcomeBuildItem);
        Map<String, JacksonJqConfig.JacksonJqModuleConfig> configuredModules = new HashMap<>(/* config.modules() */);
        List<JacksonJqModuleRegistry.ModuleDescriptor> enabledModules = new ArrayList<>();
        activeModules.forEach(activeModule -> {
            JacksonJqConfig.JacksonJqModuleConfig moduleConfig = configuredModules.get(activeModule.id());
            if (moduleConfig == null || moduleConfig.enabled()) {
                enabledModules.add(activeModule);
            }
            configuredModules.remove(activeModule.id());
        });
        configuredModules.forEach((id, configuredModule) -> {
            JacksonJqModuleRegistry.ModuleDescriptor moduleDescriptor = JacksonJqModuleRegistry.findById(id);
            if (moduleDescriptor == null) {
                LOGGER.warn("Configured module quarkus.jackson.jq.modules.{} is not recognized as a valid JacksonJq module.",
                        id);
            } else if (configuredModule.enabled()) {
                LOGGER.warn("""
                        Configured module quarkus.jackson.jq.modules.{} is enabled but was not added as a project\
                         dependency. You must add the following dependency to your project:\s
                         \
                            <dependency>
                              <groupId>{}</groupId>
                              <artifactId>{}</artifactId>
                              <version>${jackson-jq.version}</version>
                            </dependency>""", id, JacksonJqModuleRegistry.GROUP_ID, moduleDescriptor.artifactId());
            }
        });
        activeModulesBuildItemBuildProducer.produce(new ActiveJacksonJqModulesBuildItem(enabledModules));

        List<FunctionDescriptor> functions = customFunctions(combinedIndex.getIndex());
        //
        //        RuntimeValue<Environment<JsonNode>> environment = recorder.createEnvironment(config.functions().version().toString(),
        //                config.functions().excludes().orElse(new ArrayList<>()), enabledModules);

        DotName envDotName = DotName.createSimple(Environment.class.getName());
        DotName jsonNodeDotName = DotName.createSimple(JsonNode.class.getName());
        Type jsonNodeType = ClassType.create(jsonNodeDotName, Type.Kind.CLASS);
        ParameterizedType envType = ParameterizedType.create(envDotName, new Type[] { jsonNodeType }, null);

        List<JacksonJqModuleRegistry.ModuleDescriptor> unModificableEnabledModules = List.copyOf(enabledModules);

        serviceRegistrar.forService(EnvironmentSupplier.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(JacksonJqConfig.class)
                .onStart((startContext, config1) -> {
                    JacksonJqEnvironmentRecorder recorder = new JacksonJqEnvironmentRecorder();
                    Environment<JsonNode> env = recorder.createEnvironment(config1.functions().version().toString(),
                            config1.functions().excludes().orElse(new ArrayList<>()), unModificableEnabledModules);
                    return new EnvironmentSupplier(env);
                });

        syntheticBeansBuildItemProducer.produce(SyntheticBeanBuildItem
                .configure(EnvironmentSupplier.class)
                .setRuntimeInit()
                .addType(envType)
                .scope(Singleton.class)
                .runtimeValue(serviceRegistrar.serviceAsRuntimeValue(EnvironmentSupplier.class))
                .done());
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(Environment.class);
    }

    @BuildStep
    void registerModulesForNative(ActiveJacksonJqModulesBuildItem activeJacksonJqModulesBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        reflectiveClassBuildItemBuildProducer.produce(ReflectiveClassBuildItem
                .builder(activeJacksonJqModulesBuildItem.getModules().stream().map(ModuleDescriptor::className).toList())
                .build());
    }

}
