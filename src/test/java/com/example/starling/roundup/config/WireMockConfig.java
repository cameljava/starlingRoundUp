package com.example.starling.roundup.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@TestConfiguration
public class WireMockConfig {

    @Bean
    public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
        return new WireMockConfigurationCustomizer() {
            @Override
            public void customize(WireMockConfiguration config) {
                config.usingFilesUnderDirectory("src/test/resources/wiremock");
            }
        };
    }
} 