package io.github.egorkor.webutils;

import io.github.egorkor.webutils.dto.DtoMapper;
import io.github.egorkor.webutils.postProcessor.JpaServiceTemplateInheritorValidationBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@AutoConfiguration
public class AutoConfigurationSource {

    @Bean
    public DtoMapper dtoConverter() {
        return new DtoMapper();
    }

    @Bean
    public JpaServiceTemplateInheritorValidationBeanPostProcessor jpaServiceTemplateInheritorValidationBeanPostProcessor() {
        return new JpaServiceTemplateInheritorValidationBeanPostProcessor();
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

}
