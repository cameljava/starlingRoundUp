package com.example.starling.roundup.config;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class EnvironmentPrinter {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentPrinter.class);
    
    @PostConstruct
    public void printEnvironmentVariables() {
        logger.info("=== Environment Variables ===");
        Map<String, String> env = new TreeMap<>(System.getenv());
        for (Map.Entry<String, String> entry : env.entrySet()) {
            logger.info("{} = {}", entry.getKey(), entry.getValue());
        }
        logger.info("=== System Properties ===");
        Map<String, String> props = new TreeMap<>();
        for (String key : System.getProperties().stringPropertyNames()) {
            props.put(key, System.getProperty(key));
        }
        for (Map.Entry<String, String> entry : props.entrySet()) {
            logger.info("{} = {}", entry.getKey(), entry.getValue());
        }
    }
} 