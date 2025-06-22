package io.github.egorkor.webutils.postProcessor;

import io.github.egorkor.webutils.template.jpa.JpaCrudService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Arrays;

public class JpaServiceTemplateInheritorValidationBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        if (!clazz.getSuperclass().equals(JpaCrudService.class)) {
            return bean;
        }
        boolean findEntityManager = Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(EntityManager.class));
        if (!findEntityManager) {
            throw new IllegalStateException(beanName + ":" + clazz.getName() + " should have field of EntityManager type annotated by @PersistenceContext");
        }
        return bean;
    }
}
