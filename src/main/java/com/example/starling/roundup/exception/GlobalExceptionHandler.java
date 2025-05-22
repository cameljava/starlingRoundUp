package com.example.starling.roundup.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import reactor.core.publisher.Mono;

@Component
@Order(-2) // Give it higher priority than the default WebExceptionHandler
public class GlobalExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof DownstreamClientException) {
            return handleDownstreamClientError(exchange, (DownstreamClientException) ex);
        } else if (ex instanceof DownstreamServerException) {
            return handleDownstreamServerError(exchange, (DownstreamServerException) ex);
        } else if (ex instanceof InsufficientBalanceException) {
            return handleInsufficientBalance(exchange, (InsufficientBalanceException) ex);
        } else if (ex instanceof InvalidAccountDataException) {
            return handleInvalidAccountData(exchange, (InvalidAccountDataException) ex);
        } else if (ex instanceof AccountNotFoundException) {
            return handleAccountNotFound(exchange, (AccountNotFoundException) ex);
        }
        return Mono.error(ex);
    }

    private Mono<Void> handleDownstreamClientError(ServerWebExchange exchange, DownstreamClientException ex) {
        return ServerResponse.status(HttpStatus.BAD_GATEWAY)
                .bodyValue("Downstream api client error: " + ex.getMessage())
                .flatMap(response -> response.writeTo(exchange, null));
    }

    private Mono<Void> handleDownstreamServerError(ServerWebExchange exchange, DownstreamServerException ex) {
        return ServerResponse.status(HttpStatus.BAD_GATEWAY)
                .bodyValue("Downstream api server error: " + ex.getMessage())
                .flatMap(response -> response.writeTo(exchange, null));
    }

    private Mono<Void> handleInsufficientBalance(ServerWebExchange exchange, InsufficientBalanceException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "code", "InsufficientBalance",
                "message", ex.getMessage()
        );
        return ServerResponse.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyValue(body)
                .flatMap(response -> response.writeTo(exchange, null));
    }

    private Mono<Void> handleInvalidAccountData(ServerWebExchange exchange, InvalidAccountDataException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "code", "InvalidAccountData",
                "message", ex.getMessage()
        );
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue(body)
                .flatMap(response -> response.writeTo(exchange, null));
    }

    private Mono<Void> handleAccountNotFound(ServerWebExchange exchange, AccountNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "code", "AccountNotFound",
                "message", ex.getMessage()
        );
        return ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue(body)
                .flatMap(response -> response.writeTo(exchange, null));
    }
}
