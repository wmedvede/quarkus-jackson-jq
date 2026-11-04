package io.quarkiverse.jackson.jq.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkiverse.jackson.jq.EnabledModule;
import io.quarkiverse.jackson.jq.EnvironmentProducer;
import io.quarkiverse.jackson.jq.EnvironmentSupplier;
import io.quarkiverse.jackson.jq.FilteringJavaModule;
import io.quarkiverse.jackson.jq.FilteringJqModule;
import io.quarkiverse.jackson.jq.FunctionDescriptor;
import io.quarkiverse.jackson.jq.FunctionFilter;
import io.quarkiverse.jackson.jq.JacksonJqEnvironmentCreator;
import io.quarkiverse.jackson.jq.JacksonJqModuleRegistry;
import io.quarkiverse.jackson.jq.ModuleDescriptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistrations;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
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
        Map<String, JacksonJqConfig.JacksonJqModuleConfig> unprocessedModules = new HashMap<>(config.modules());
        AtomicReference<ModuleDescriptor> joniWasEnabled = new AtomicReference<>();
        AtomicReference<ModuleDescriptor> re2WasEnabled = new AtomicReference<>();

        curateOutcome.getApplicationModel().getRuntimeDependencies()
                .forEach(dep -> {
                    if (JacksonJqModuleRegistry.GROUP_ID.equals(dep.getGroupId())) {
                        ModuleDescriptor moduleDescriptor = JacksonJqModuleRegistry.findByArtifactId(dep.getArtifactId());
                        if (moduleDescriptor != null) {
                            JacksonJqConfig.JacksonJqModuleConfig moduleConfig = unprocessedModules.get(moduleDescriptor.id());
                            if (moduleConfig == null || moduleConfig.enabled()) {
                                JacksonJqConfig.Strategy strategy = moduleConfig != null ? moduleConfig.strategy()
                                        : JacksonJqConfig.DEFAULT_STRATEGY;
                                Optional<String> alias = moduleConfig != null ? moduleConfig.alias() : Optional.empty();
                                Optional<String> path = moduleConfig != null ? moduleConfig.path() : Optional.empty();
                                if (strategy == JacksonJqConfig.Strategy.IMPORT) {
                                    String aliasValue = alias.orElseThrow(() -> new IllegalArgumentException(String.format(
                                            "JacksonJq module configuration \"quarkus.jackson.jq\".%s with the strategy = 'import' requires configuring the 'alias' property. You must add the property \"quarkus.jackson.jq\".%s.alias with a non-empty value.",
                                            moduleDescriptor.id(), moduleDescriptor.id())));
                                    moduleBuildItemBuildProducer
                                            .produce(JacksonJqModuleBuildItem.forImport(moduleDescriptor, aliasValue));
                                } else if (strategy == JacksonJqConfig.Strategy.REGISTER) {
                                    moduleBuildItemBuildProducer.produce(JacksonJqModuleBuildItem.forRegister(moduleDescriptor,
                                            path.orElse(null)));
                                } else {
                                    moduleBuildItemBuildProducer.produce(JacksonJqModuleBuildItem.forInclude(moduleDescriptor));
                                }
                                if (JacksonJqModuleRegistry.JONI_MODULE_ARTIFACT_ID.equals(moduleDescriptor.artifactId())) {
                                    joniWasEnabled.set(moduleDescriptor);
                                } else if (JacksonJqModuleRegistry.RE2_MODULE_ARTIFACT_ID
                                        .equals(moduleDescriptor.artifactId())) {
                                    re2WasEnabled.set(moduleDescriptor);
                                }
                            }
                            unprocessedModules.remove(moduleDescriptor.id());
                        }
                    }
                });

        if (joniWasEnabled.get() != null && re2WasEnabled.get() != null) {
            throw new IllegalArgumentException(
                    "JoniRegexModule and Re2RegexModule cannot be enabled at the same time. Disable one by setting either 'quarkus.jackson.jq.modules.joni.enabled=false' or 'quarkus.jackson.jq.modules.re2.enabled=false'.");
        }
        unprocessedModules.forEach((id, configuredModule) -> {
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
    }

    @BuildStep
    void scanFunctions(ApplicationIndexBuildItem applicationIndex,
            BuildProducer<JacksonJqFunctionBuildItem> functionBuildItemBuildProducer) {
        DotName functionName = DotName.createSimple(Function.class.getName());
        DotName functionRegistrationName = DotName.createSimple(FunctionRegistration.class.getName());
        DotName functionRegistrationsName = DotName.createSimple(FunctionRegistrations.class.getName());
        applicationIndex.getIndex().getAllKnownImplementations(functionName)
                .forEach(functionClass -> {
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
                    functionRegistrations.forEach(
                            annotationInstance -> functionBuildItemBuildProducer.produce(new JacksonJqFunctionBuildItem(
                                    newFunctionDescriptor(functionClass.name().toString(), annotationInstance))));
                });
    }

    @BuildStep
    void registerEnvironmentBeans(JacksonJqConfig config, List<JacksonJqModuleBuildItem> moduleBuildItems,
            List<JacksonJqFunctionBuildItem> functionBuildItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeansBuildItemProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            ServiceRegistrar serviceRegistrar) {

        String version = config.functions().version().toString();
        List<String> excludes = Collections.unmodifiableList(config.functions().excludes().orElse(new ArrayList<>()));
        List<EnabledModule> enabledModules = moduleBuildItems.stream()
                .map(moduleBuildItem -> new EnabledModule(moduleBuildItem.getModuleDescriptor(),
                        generateFilteringClassName(moduleBuildItem.getModuleDescriptor().className()),
                        moduleBuildItem.getStrategy().name(),
                        moduleBuildItem.getAlias(),
                        moduleBuildItem.getPath()))
                .toList();
        List<FunctionDescriptor> customFunctions = functionBuildItems.stream()
                .map(JacksonJqFunctionBuildItem::getFunctionDescriptor).toList();

        serviceRegistrar.forService(EnvironmentSupplier.class, "environment-supplier")
                .atPhase(Phase.STATIC_INIT)
                .onStart((startContext) -> {
                    Environment<JsonNode> env = JacksonJqEnvironmentCreator.createEnvironment(version, excludes, enabledModules,
                            customFunctions);
                    return new EnvironmentSupplier(env);
                });

        syntheticBeansBuildItemProducer.produce(SyntheticBeanBuildItem.configure(EnvironmentSupplier.class)
                .serviceValue(EnvironmentSupplier.class, "environment-supplier")
                .scope(Singleton.class)
                .done());

        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(EnvironmentProducer.class));
    }

    @BuildStep
    void generateFilteringModules(List<JacksonJqModuleBuildItem> moduleBuildItems,
            BuildProducer<GeneratedClassBuildItem> generatedClassProducer) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClassProducer, true);
        for (JacksonJqModuleBuildItem moduleBuildItem : moduleBuildItems) {
            String inputClassName = moduleBuildItem.getModuleDescriptor().className();
            try {
                Class<?> inputClass = Thread.currentThread().getContextClassLoader().loadClass(inputClassName);
                Class<?> baseFilteringClass;
                Class<?> baseModuleClass;
                if (JavaModule.class.isAssignableFrom(inputClass)) {
                    baseFilteringClass = FilteringJavaModule.class;
                    baseModuleClass = JavaModule.class;
                } else {
                    baseFilteringClass = FilteringJqModule.class;
                    baseModuleClass = JqModule.class;
                }
                String generatedClassName = generateFilteringClassName(inputClass.getName());
                try (ClassCreator classCreator = ClassCreator.builder()
                        .classOutput(classOutput)
                        .className(generatedClassName)
                        .superClass(baseFilteringClass)
                        .build()) {

                    for (ModuleRegistration registration : inputClass.getAnnotationsByType(ModuleRegistration.class)) {
                        classCreator.addAnnotation(ModuleRegistration.class)
                                .addValue("path", registration.path());
                    }

                    try (MethodCreator ctor = classCreator.getConstructorCreator(FunctionFilter.class, String.class)) {
                        ctor.setModifiers(Modifier.PUBLIC);

                        ResultHandle thisHandle = ctor.getThis();
                        ResultHandle delegateHandle = ctor.newInstance(MethodDescriptor.ofConstructor(inputClass));
                        ResultHandle filterHandle = ctor.getMethodParam(0);
                        ResultHandle locatorHandle = ctor.getMethodParam(1);

                        ctor.invokeSpecialMethod(
                                MethodDescriptor.ofConstructor(baseFilteringClass, baseModuleClass,
                                        FunctionFilter.class, String.class),
                                thisHandle,
                                delegateHandle,
                                filterHandle,
                                locatorHandle);
                        ctor.returnVoid();
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to load module class for Gizmo generation: " + inputClassName, e);
            }
        }
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
        if (major < 0 && minor < 0 && patch < 0) {
            // un-bounded version
            return null;
        }
        return Version.of(Math.max(major, 0), Math.max(minor, 0), Math.max(patch, 0));
    }

    private static Version parseVersion(AnnotationInstance annotationInstance, String valueName) {
        if (annotationInstance.value(valueName) != null) {
            return parseVersion(annotationInstance.value(valueName).asNested());
        }
        return null;
    }

    private static VersionRange parseVersionRange(AnnotationInstance versionRangeSpecInstance) {
        Version min = parseVersion(versionRangeSpecInstance, "min");
        boolean minInclusive = parseBoolean(versionRangeSpecInstance, "minInclusive", true);
        Version max = parseVersion(versionRangeSpecInstance, "max");
        boolean maxInclusive = parseBoolean(versionRangeSpecInstance, "maxInclusive", false);
        return VersionRange.of(min, minInclusive, max, maxInclusive);
    }

    private static boolean parseBoolean(AnnotationInstance annotationInstance, String valueName, boolean defaultValue) {
        if (annotationInstance.value(valueName) != null) {
            return annotationInstance.value(valueName).asBoolean();
        }
        return defaultValue;
    }

    private static String generateFilteringClassName(String className) {
        int lastSeparator = className.lastIndexOf(".");
        String simpleClassName = className.substring(lastSeparator + 1);
        return "io.quarkiverse.jackson.jq.modules.Filtering" + simpleClassName;
    }
}
