package com.example.starling.roundup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Print important environment variables to the log
 * only used for dev profile, to check the environment variables are set correctly
 * @see <a href="https://www.baeldung.com/spring-environment-and-profile">Spring Environment and Profile</a>
 */
@Configuration
@Profile("dev")
public class EnvTestConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvTestConfig.class);
    
    @Value("${starling.api.token:NOT_SET}")
    private String apiToken;
    
    @Value("${starling.api.url:NOT_SET}")
    private String apiUrl;
    
    @PostConstruct
    public void logEnvVariables() {
        logger.info("STARLING_API_TOKEN: {}", apiToken);
        logger.info("STARLING_API_URL: {}", apiUrl);
        logger.info("Active profile: {}", System.getProperty("spring.profiles.active"));
    }
} 