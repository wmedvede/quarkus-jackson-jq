package io.quarkiverse.jackson.jq.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.recording.RecorderContext;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

final class JacksonJqSupport {
    private JacksonJqSupport() {
    }

    static List<JacksonJqFunctionBuildItem> lookupFunctions(
            IndexView indexView,
            JacksonJqConfig config,
            RecorderContext context,
            Class<?> annotationType) {

        List<JacksonJqFunctionBuildItem> answer = new ArrayList<>();

        DotName f = DotName.createSimple(Function.class.getName());
        DotName a = DotName.createSimple(annotationType.getName());

        for (ClassInfo ci : indexView.getAllKnownImplementations(f)) {
            AnnotationInstance annotation = ci.declaredAnnotation(a);
            if (annotation == null) {
                continue;
            }

            AnnotationValue values = annotation.value("value");
            AnnotationValue version = annotation.value("version");

            for (String name : values.asStringArray()) {
                var function = JacksonJqFunctionBuildItem.of(
                        name,
                        context.newInstance(ci.name().toString()));

                if (version != null && !version.asString().isEmpty()) {
                    if (!VersionRange.valueOf(version.asString()).contains(config.functions().version())) {
                        continue;
                    }
                }

                answer.add(function);
            }
        }

        return answer;
    }
}
