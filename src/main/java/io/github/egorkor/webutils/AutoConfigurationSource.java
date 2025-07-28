package io.github.egorkor.webutils;

import io.github.egorkor.webutils.analyze.jpa.JpaCatalogEntityMetaAnalyzer;
import io.github.egorkor.webutils.analyze.jpa.ModelMeta;
import io.github.egorkor.webutils.analyze.jpa.ModelMetaHolder;
import io.github.egorkor.webutils.dto.DtoMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

import java.util.Map;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@AutoConfiguration
@RequiredArgsConstructor
public class AutoConfigurationSource {
    private final EntityManager entityManager;
    private final ApplicationContext applicationContext;

    @Bean
    public DtoMapper dtoConverter() {
        return new DtoMapper();
    }

    @Bean
    public ModelMetaHolder jpaServiceTemplateInheritorValidationBeanPostProcessor() {
        Map<Class<?>, ModelMeta> meta = JpaCatalogEntityMetaAnalyzer.getMeta(entityManager, applicationContext);
        return new ModelMetaHolder(meta);
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

}
