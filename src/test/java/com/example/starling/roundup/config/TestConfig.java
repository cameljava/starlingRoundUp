package com.example.starling.roundup.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.example.starling.roundup.exception.DownstreamApiErrorHandler;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .uriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:${wiremock.server.port}"))
                .errorHandler(new DownstreamApiErrorHandler())
                .build();
    }
} 