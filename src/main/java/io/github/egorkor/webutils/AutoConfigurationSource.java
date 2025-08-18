package io.github.egorkor.webutils;

import io.github.egorkor.webutils.api.GenericApiControllerAdvice;
import io.github.egorkor.webutils.dto.DtoMapper;
import io.github.egorkor.webutils.postProcessor.JpaServiceTemplateInheritorValidationBeanPostProcessor;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration
@RequiredArgsConstructor
public class AutoConfigurationSource {
    private final EntityManager entityManager;
    private final ApplicationContext applicationContext;


    @ConditionalOnMissingBean(GenericApiControllerAdvice.class)
    @Bean
    public GenericApiControllerAdvice genericApiControllerAdvice(){
        return new GenericApiControllerAdvice();
    }

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public Validator validator(@Autowired LocalValidatorFactoryBean validatorFactoryBean) {
        return validatorFactoryBean.getValidator();
    }

    @Bean
    public DtoMapper dtoConverter() {
        return new DtoMapper();
    }

    @Role(value = BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    public JpaServiceTemplateInheritorValidationBeanPostProcessor jpaServiceTemplateInheritorValidationBeanPostProcessor() {
        return new JpaServiceTemplateInheritorValidationBeanPostProcessor();
    }

    @Role(value = BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

}
