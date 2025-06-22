package io.github.egorkor.webutils.postProcessor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

public class PersistenceContextAnnotationValidationBeanPostProcessor
        implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!isBeanEligibleForValidation(bean)) {
            return bean;
        }
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.getType().equals(EntityManager.class)) {
                continue;
            }
            if (!field.isAnnotationPresent(PersistenceContext.class)) {
                throw new IllegalStateException(beanName
                        + ":"
                        + bean.getClass()
                        + ":"
                        + field.getName()
                        + ": EntityManager field should be annotated by @PersistenceContext");
            }
        }
        return bean;
    }

    private boolean isBeanEligibleForValidation(Object bean) {
        Class<?> clazz = bean.getClass();
        return clazz.isAnnotationPresent(Service.class)
                || clazz.isAnnotationPresent(Repository.class)
                || clazz.isAnnotationPresent(Component.class);
    }
}
