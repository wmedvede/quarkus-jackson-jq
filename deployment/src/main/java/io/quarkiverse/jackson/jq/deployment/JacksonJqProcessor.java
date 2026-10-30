package io.quarkiverse.jackson.jq.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkiverse.jackson.jq.EnvironmentProducer;
import io.quarkiverse.jackson.jq.EnvironmentSupplier;
import io.quarkiverse.jackson.jq.FunctionDescriptor;
import io.quarkiverse.jackson.jq.JacksonJqEnvironmentRecorder;
import io.quarkiverse.jackson.jq.JacksonJqModuleRegistry;
import io.quarkiverse.jackson.jq.ModuleDescriptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import tools.jackson.databind.JsonNode;

class JacksonJqProcessor {
    private static final String FEATURE = "jackson-jq";
    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonJqProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void scanModules(JacksonJqConfig config, CurateOutcomeBuildItem curateOutcome,
            BuildProducer<JacksonJqModuleBuildItem> moduleBuildItemBuildProducer) {
        List<ModuleDescriptor> enabledModules = new ArrayList<>();
        Map<String, JacksonJqConfig.JacksonJqModuleConfig> unProcessedModules = new HashMap<>(config.modules());
        curateOutcome.getApplicationModel().getRuntimeDependencies()
                .forEach(dep -> {
                    if (JacksonJqModuleRegistry.GROUP_ID.equals(dep.getGroupId())) {
                        ModuleDescriptor moduleDescriptor = JacksonJqModuleRegistry.findByArtifactId(dep.getArtifactId());
                        if (moduleDescriptor != null) {
                            JacksonJqConfig.JacksonJqModuleConfig moduleConfig = unProcessedModules.get(moduleDescriptor.id());
                            if (moduleConfig == null || moduleConfig.enabled()) {
                                enabledModules.add(moduleDescriptor);
                            }
                            unProcessedModules.remove(moduleDescriptor.id());
                        }
                    }
                });

        unProcessedModules.forEach((id, configuredModule) -> {
            ModuleDescriptor moduleDescriptor = JacksonJqModuleRegistry.findById(id);
            if (moduleDescriptor == null) {
                LOGGER.warn("Configured module quarkus.jackson.jq.modules.{} is not recognized as a valid JacksonJq module.",
                        id);
            } else if (configuredModule.enabled()) {
                LOGGER.warn(
                        """
                                Configured JacksonJq extension module {} is enabled via the quarkus.jackson.jq.modules.{}.enabled=true property but was not added as a project\
                                 dependency. You must add the following dependency to your project:\s
                                 \
                                    <dependency>
                                      <groupId>{}</groupId>
                                      <artifactId>{}</artifactId>
                                      <version>${jackson-jq.version}</version>
                                    </dependency>""",
                        id, id, JacksonJqModuleRegistry.GROUP_ID, moduleDescriptor.artifactId());
            }
        });
        enabledModules
                .forEach(enabledModule -> moduleBuildItemBuildProducer.produce(new JacksonJqModuleBuildItem(enabledModule)));
    }

    @BuildStep
    void scanFunctions(CombinedIndexBuildItem combinedIndex,
            BuildProducer<JacksonJqFunctionBuildItem> functionBuildItemBuildProducer) {
        DotName functionName = DotName.createSimple(Function.class.getName());
        DotName functionRegistrationName = DotName.createSimple(FunctionRegistration.class.getName());
        DotName functionRegistrationsName = DotName.createSimple(FunctionRegistrations.class.getName());
        combinedIndex.getIndex().getAllKnownImplementations(functionName)
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
                        functionBuildItemBuildProducer.produce(new JacksonJqFunctionBuildItem(
                                newFunctionDescriptor(functionClass.name().toString(), annotationInstance)));
                    });
                });
    }

    @BuildStep
    void quarkusEnvironmentBean(JacksonJqConfig config, List<JacksonJqModuleBuildItem> moduleBuildItems,
            List<JacksonJqFunctionBuildItem> functionBuildItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeansBuildItemProducer,
            ServiceRegistrar serviceRegistrar) {

        DotName envDotName = DotName.createSimple(Environment.class.getName());
        DotName jsonNodeDotName = DotName.createSimple(JsonNode.class.getName());
        Type jsonNodeType = ClassType.create(jsonNodeDotName, Type.Kind.CLASS);
        ParameterizedType envType = ParameterizedType.create(envDotName, new Type[] { jsonNodeType }, null);

        String version = config.functions().version().toString();
        List<String> excludes = config.functions().excludes().orElse(new ArrayList<>()).stream().toList();
        List<ModuleDescriptor> enabledModules = moduleBuildItems.stream().map(JacksonJqModuleBuildItem::getModuleDescriptor)
                .toList();
        List<FunctionDescriptor> customFunctions = functionBuildItems.stream()
                .map(JacksonJqFunctionBuildItem::getFunctionDescriptor).toList();

        serviceRegistrar.forService(EnvironmentSupplier.class)
                .atPhase(Phase.STATIC_INIT)
                .onStart((startContext) -> {
                    JacksonJqEnvironmentRecorder recorder = new JacksonJqEnvironmentRecorder();
                    Environment<JsonNode> env = recorder.createEnvironment(version, excludes, enabledModules, customFunctions);
                    return new EnvironmentSupplier(env);
                });

        /*
         * serviceRegistrar.forService(EnvironmentProducer.class)
         * .atPhase(Phase.STATIC_INIT)
         * .require(EnvironmentSupplier.class)
         * .onStart((startContext, environmentSupplier) -> new EnvironmentProducer(environmentSupplier));
         */

        syntheticBeansBuildItemProducer.produce(SyntheticBeanBuildItem.configure(EnvironmentSupplier.class)
                .serviceValue(EnvironmentSupplier.class/* , "my-bean-service" */)
                .scope(Singleton.class)
                .done());

        /*
         * syntheticBeansBuildItemProducer.produce(SyntheticBeanBuildItem.configure(EnvironmentProducer.class)
         * .serviceValue(EnvironmentProducer.class/* , "my-bean-service" )
         * .scope(Singleton.class)
         * .done());
         */
    }

    @BuildStep
    void registerEnvironmentProducer(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(EnvironmentProducer.class));
    }

    @BuildStep
    void registerForNative(List<JacksonJqModuleBuildItem> moduleBuildItems,
            List<JacksonJqFunctionBuildItem> functionBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        moduleBuildItems.forEach(moduleItem -> reflectiveClassBuildItemBuildProducer
                .produce(ReflectiveClassBuildItem.builder(moduleItem.getModuleDescriptor().className()).build()));
        functionBuildItems.forEach(functionItem -> reflectiveClassBuildItemBuildProducer
                .produce(ReflectiveClassBuildItem.builder(functionItem.getFunctionDescriptor().className()).build()));
    }

    private static FunctionDescriptor newFunctionDescriptor(String className, AnnotationInstance annotation) {
        String name = annotation.value("name").asString();
        int nargs = annotation.value("nargs").asInt();
        VersionRange versionRange = VersionRange.of(null, true, null, false);
        AnnotationValue version = annotation.value("version");
        if (version != null) {
            versionRange = parseVersionRange(version.asNested());
        }
        return new FunctionDescriptor(name, nargs, className, versionRange.toString());
    }

    private static Version parseVersion(AnnotationInstance versionInstance) {
        int major = versionInstance.value("major").asInt();
        int minor = versionInstance.value("minor").asInt();
        int patch = versionInstance.value("patch").asInt();
        return Version.of(Math.max(major, 0), Math.max(minor, 0), Math.max(patch, 0));
    }

    private static VersionRange parseVersionRange(AnnotationInstance versionRangeSpecInstance) {
        Version min = parseVersion(versionRangeSpecInstance.value("min").asNested());
        boolean minInclusive = true;
        if (versionRangeSpecInstance.value("minInclusive") != null) {
            minInclusive = versionRangeSpecInstance.value("minInclusive").asBoolean();
        }
        Version max = parseVersion(versionRangeSpecInstance.value("max").asNested());
        boolean maxInclusive = false;
        if (versionRangeSpecInstance.value("maxInclusive") != null) {
            maxInclusive = versionRangeSpecInstance.value("maxInclusive").asBoolean();
        }
        return VersionRange.of(min, minInclusive, max, maxInclusive);
    }
}
