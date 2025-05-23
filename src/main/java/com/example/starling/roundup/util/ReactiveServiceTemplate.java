package com.example.starling.roundup.util;

import java.time.Duration;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.starling.roundup.exception.InvalidAccountDataException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
public class ReactiveServiceTemplate {

    public static <T> Mono<T> withRetry(Mono<T> operation, String operationName) {
        return operation
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(e -> shouldRetry(e))
                        .doBeforeRetry(retrySignal
                                -> log.warn("Retrying {} after error: {}",
                                operationName,
                                retrySignal.failure().getMessage())))
                .onErrorMap(e -> {
                    String errorMessage = e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += ": " + e.getCause().getMessage();
                    }
                    log.error("Failed to execute {}: {}", operationName, errorMessage);
                    return new InvalidAccountDataException(
                            String.format("Failed to execute %s: %s", operationName, errorMessage));
                });
    }

    private static boolean shouldRetry(Throwable e) {
        if (e instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError();
        }
        return e instanceof WebClientRequestException;
    }
}
