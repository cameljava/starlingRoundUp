package com.example.starling.roundup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.example.starling.roundup.exception.DownstreamApiErrorHandler;

@Configuration
public class RestTemplateConfig {

    @Value("${starling.api.token}")
    private String apiToken;

    @Value("${starling.api.url}")
    private String apiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .uriTemplateHandler(new DefaultUriBuilderFactory(apiUrl))
                .errorHandler(new DownstreamApiErrorHandler())
                .build();
    }
}
