package io.github.egorkor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AutoConfigurationSource {

    @Bean
    public DtoConverter dtoConverter() {
        return new DtoConverter();
    }


}
