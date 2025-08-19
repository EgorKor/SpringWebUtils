package io.github.egorkor.webutils.condition;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.Annotation;
import java.util.Map;

public class ConditionUtils {

    public static boolean isBeanWithAnnotationPresents(ConditionContext context,
                                                       AnnotatedTypeMetadata metadata,
                                                       Class<? extends Annotation> annotation) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory == null) {
            return false;
        }
        Map<String, Object> beansWithAnnotation =
                beanFactory.getBeansWithAnnotation(annotation);
        return !beansWithAnnotation.isEmpty();
    }
}
