package com.rachana.tapewatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaskerConfig {
    @Bean Masker masker(@Value("${surveillance.masking-secret}") String secret) { return new Masker(secret); }
}
