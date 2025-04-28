package com.example.starling.roundup.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    //Assumption: further effort to work on how to handle errors: log, alarm, retry or fix queue.
  
    // As all downstreams are interal apis, not sure if 502 is proper statusCode, may need further discussion
    @ExceptionHandler(DownstreamClientException.class)
    public ResponseEntity<String> handleDownstreamClientError(DownstreamClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Downstream api client error: " + ex.getMessage());
    }

    // As all downstreams are interal apis, not sure if 502 is proper statusCode, may need further discussion
    @ExceptionHandler(DownstreamServerException.class)
    public ResponseEntity<String> handleDownstreamServerError(DownstreamServerException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Downstream api server error: " + ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Object> handleInsufficientBalance(InsufficientBalanceException ex) {
        Map<String, Object> body = Map.of(
            "timestamp", Instant.now(),
            "code", "InsufficientBalance",
            "message", ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY) // 422
                .body(body);
    }

    @ExceptionHandler(InvalidAccountDataException.class)
    public ResponseEntity<Object> handleInvalidAccountData(InvalidAccountDataException ex) {
        Map<String, Object> body = Map.of(
            "timestamp", Instant.now(),
            "code", "InvalidAccountData",
            "message", ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Object> handleAccountNotFound(AccountNotFoundException ex) {
        Map<String, Object> body = Map.of(
            "timestamp", Instant.now(),
            "code", "AccountNotFound",
            "message", ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }
}
