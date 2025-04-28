package com.example.starling.roundup.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDownstreamClientError() {
        DownstreamClientException ex = new DownstreamClientException("client error");
        ResponseEntity<String> resp = handler.handleDownstreamClientError(ex);
        assertEquals(HttpStatus.BAD_GATEWAY, resp.getStatusCode());
        assertEquals("Downstream api client error: client error", resp.getBody());
    }

    @Test
    void handleDownstreamServerError() {
        DownstreamServerException ex = new DownstreamServerException("server error");
        ResponseEntity<String> resp = handler.handleDownstreamServerError(ex);
        assertEquals(HttpStatus.BAD_GATEWAY, resp.getStatusCode());
        assertEquals("Downstream api server error: server error", resp.getBody());
    }

    @Test
    void handleInsufficientBalance() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Insufficient funds");
        ResponseEntity<Object> resp = handler.handleInsufficientBalance(ex);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof Instant);
        assertEquals("InsufficientBalance", body.get("code"));
        assertEquals("Insufficient funds", body.get("message"));
    }

    @Test
    void handleInvalidAccountData() {
        InvalidAccountDataException ex = new InvalidAccountDataException("bad data");
        ResponseEntity<Object> resp = handler.handleInvalidAccountData(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof Instant);
        assertEquals("InvalidAccountData", body.get("code"));
        assertEquals("bad data", body.get("message"));
    }
}
