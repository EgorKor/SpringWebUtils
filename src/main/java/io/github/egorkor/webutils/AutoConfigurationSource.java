package io.github.egorkor.webutils;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AutoConfigurationSource {

    @Bean
    public DtoMapper dtoConverter() {
        return new DtoMapper();
    }

}
