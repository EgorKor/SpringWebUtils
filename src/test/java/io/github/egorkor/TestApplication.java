package io.github.egorkor;

import io.github.egorkor.webutils.postProcessor.JpaServiceTemplateInheritorValidationBeanPostProcessor;
import io.github.egorkor.webutils.postProcessor.PersistenceContextAnnotationValidationBeanPostProcessor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;


@SpringBootApplication
public class TestApplication {

    @Profile("test")
    @Bean
    public JpaServiceTemplateInheritorValidationBeanPostProcessor jpaBeanPostProcessor() {
        return new JpaServiceTemplateInheritorValidationBeanPostProcessor();
    }

    @Profile("test")
    @Bean
    public PersistenceContextAnnotationValidationBeanPostProcessor persistenceBeanPostProcessor() {
        return new PersistenceContextAnnotationValidationBeanPostProcessor();
    }
    
}
