package io.github.egorkor;

import io.github.egorkor.webutils.postProcessor.JpaServiceTemplateInheritorValidationBeanPostProcessor;
import io.github.egorkor.webutils.postProcessor.PersistenceContextAnnotationValidationBeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApplication {

    @Bean
    public JpaServiceTemplateInheritorValidationBeanPostProcessor jpaBeanPostProcessor() {
        return new JpaServiceTemplateInheritorValidationBeanPostProcessor();
    }

    @Bean
    public PersistenceContextAnnotationValidationBeanPostProcessor persistenceBeanPostProcessor() {
        return new PersistenceContextAnnotationValidationBeanPostProcessor();
    }
}
