package com.example.starling.roundup.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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
        exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"error\":\"Downstream api client error: " + ex.getMessage() + "\"}").getBytes()
                ))
        );
    }

    private Mono<Void> handleDownstreamServerError(ServerWebExchange exchange, DownstreamServerException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"error\":\"Downstream api server error: " + ex.getMessage() + "\"}").getBytes()
                ))
        );
    }

    private Mono<Void> handleInsufficientBalance(ServerWebExchange exchange, InsufficientBalanceException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "code", "InsufficientBalance",
                "message", ex.getMessage()
        );
        exchange.getResponse().setStatusCode(HttpStatus.UNPROCESSABLE_ENTITY);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"timestamp\":\"" + body.get("timestamp") + "\",\"code\":\"" + body.get("code") + "\",\"message\":\"" + body.get("message") + "\"}").getBytes()
                ))
        );
    }

    private Mono<Void> handleInvalidAccountData(ServerWebExchange exchange, InvalidAccountDataException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "code", "InvalidAccountData",
                "message", ex.getMessage()
        );
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"timestamp\":\"" + body.get("timestamp") + "\",\"code\":\"" + body.get("code") + "\",\"message\":\"" + body.get("message") + "\"}").getBytes()
                ))
        );
    }

    private Mono<Void> handleAccountNotFound(ServerWebExchange exchange, AccountNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "code", "AccountNotFound",
                "message", ex.getMessage()
        );
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"timestamp\":\"" + body.get("timestamp") + "\",\"code\":\"" + body.get("code") + "\",\"message\":\"" + body.get("message") + "\"}").getBytes()
                ))
        );
    }
}
