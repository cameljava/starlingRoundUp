package com.example.starling.roundup.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.starling.roundup.exception.InvalidAccountDataException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ReactiveServiceTemplateTest {

    @Test
    void withRetry_shouldSucceedOnFirstAttempt() {
        // Given
        Mono<String> successfulOperation = Mono.just("success");

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(successfulOperation, "test-operation"))
                .expectNext("success")
                .verifyComplete();
    }

    @Test
    void withRetry_shouldRetryOn5xxServerError() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Mono<String> operationWithRetry = Mono.defer(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                return Mono.error(WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, null, null));
            }
            return Mono.just("success");
        });

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(operationWithRetry, "test-operation"))
                .expectNext("success")
                .verifyComplete();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void withRetry_shouldRetryOnWebClientRequestException() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Mono<String> operationWithRetry = Mono.defer(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                return Mono.error(WebClientResponseException.create(
                        503, "Service Unavailable",
                        HttpHeaders.EMPTY,
                        null,
                        null
                ));
            }
            return Mono.just("success");
        });

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(operationWithRetry, "test-operation"))
                .expectNext("success")
                .verifyComplete();

        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void withRetry_shouldNotRetryOn4xxClientError() {
        // Given
        Mono<String> operationWith4xxError = Mono.error(
                WebClientResponseException.create(404, "Not Found", HttpHeaders.EMPTY, null, null));

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(operationWith4xxError, "test-operation"))
                .expectErrorMatches(error
                        -> error instanceof InvalidAccountDataException
                && error.getMessage().contains("Failed to execute test-operation"))
                .verify();
    }

    @Test
    void withRetry_shouldNotRetryOnNonWebClientException() {
        // Given
        Mono<String> operationWithGenericError = Mono.error(new RuntimeException("Generic error"));

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(operationWithGenericError, "test-operation"))
                .expectErrorMatches(error
                        -> error instanceof InvalidAccountDataException
                && error.getMessage().contains("Failed to execute test-operation"))
                .verify();
    }

    @Test
    void withRetry_shouldFailAfterMaxRetries() {
        // Given
        Mono<String> alwaysFailingOperation = Mono.error(
                WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, null, null));

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(alwaysFailingOperation, "test-operation"))
                .expectErrorMatches(error
                        -> error instanceof InvalidAccountDataException
                && error.getMessage().contains("Failed to execute test-operation"))
                .verify();
    }

    @Test
    void withRetry_shouldRespectBackoffDelay() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Mono<String> operationWithRetry = Mono.defer(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                return Mono.error(WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, null, null));
            }
            return Mono.just("success");
        });

        // When & Then
        StepVerifier.withVirtualTime(()
                -> ReactiveServiceTemplate.withRetry(operationWithRetry, "test-operation"))
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(1)) // First retry delay
                .thenAwait(Duration.ofSeconds(2)) // Second retry delay (exponential backoff)
                .expectNext("success")
                .verifyComplete();
    }

    @Test
    void withRetry_shouldMapErrorCorrectly() {
        // Given
        WebClientResponseException originalException = WebClientResponseException.create(
                500, "Internal Server Error", HttpHeaders.EMPTY, null, null);
        Mono<String> alwaysFailingOperation = Mono.error(originalException);

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(alwaysFailingOperation, "database-operation"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(InvalidAccountDataException.class);
                    assertThat(error.getMessage()).contains("Failed to execute database-operation");
                    assertThat(error.getMessage()).contains("500 Internal Server Error");
                })
                .verify();
    }

    @Test
    void withRetry_shouldHandleMixedRetryableAndNonRetryableErrors() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Mono<String> operationWithMixedErrors = Mono.defer(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                return Mono.error(WebClientResponseException.create(500, "Server Error", HttpHeaders.EMPTY, null, null));
            } else if (attempt == 2) {
                return Mono.error(new WebClientRequestException(new RuntimeException("Timeout"), null, null, HttpHeaders.EMPTY));
            }
            return Mono.just("success");
        });

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(operationWithMixedErrors, "test-operation"))
                .expectNext("success")
                .verifyComplete();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void withRetry_shouldPreserveOperationResult() {
        // Given
        ComplexObject expectedResult = new ComplexObject("test", 42);
        Mono<ComplexObject> successfulOperation = Mono.just(expectedResult);

        // When & Then
        StepVerifier.create(ReactiveServiceTemplate.withRetry(successfulOperation, "test-operation"))
                .expectNext(expectedResult)
                .verifyComplete();
    }

    // Helper class for testing generic types
    private static class ComplexObject {

        final String name;
        final int value;

        ComplexObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ComplexObject that = (ComplexObject) obj;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + value;
        }
    }
}
