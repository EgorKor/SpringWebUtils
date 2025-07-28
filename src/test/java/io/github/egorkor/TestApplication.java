package io.github.egorkor;

import io.github.egorkor.webutils.postProcessor.JpaServiceTemplateInheritorValidationBeanPostProcessor;
import io.github.egorkor.webutils.postProcessor.PersistenceContextAnnotationValidationBeanPostProcessor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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

    @Bean
    public Validator validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }
}
