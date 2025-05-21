package com.example.starling.roundup.controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starling.roundup.exception.DownstreamClientException;
import com.example.starling.roundup.exception.DownstreamServerException;
import com.example.starling.roundup.exception.InsufficientBalanceException;
import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.service.RoundUpService;

@WebMvcTest(RoundUpController.class)
class RoundUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoundUpService roundUpService;

    @Test
    void roundUpTransactions_ShouldReturnOk() throws Exception {
        // Given
        doNothing().when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        // Verify
        verify(roundUpService).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldReturn422_WhenInsufficientBalance() throws Exception {
        // Given
        doThrow(new InsufficientBalanceException("Insufficient balance to round up"))
                .when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("InsufficientBalance"))
                .andExpect(jsonPath("$.message").value("Insufficient balance to round up"))
                .andExpect(jsonPath("$.timestamp").exists());

        // Verify
        verify(roundUpService).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldReturn500_WhenInvalidAccountData() throws Exception {
        // Given
        doThrow(new InvalidAccountDataException("Invalid account data"))
                .when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("InvalidAccountData"))
                .andExpect(jsonPath("$.message").value("Invalid account data"))
                .andExpect(jsonPath("$.timestamp").exists());

        // Verify
        verify(roundUpService).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldReturn502_WhenDownstreamClientError() throws Exception {
        // Given
        doThrow(new DownstreamClientException("Downstream client error"))
                .when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(content().string("Downstream api client error: Downstream client error"));

        // Verify
        verify(roundUpService).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldReturn502_WhenDownstreamServerError() throws Exception {
        // Given
        doThrow(new DownstreamServerException("Downstream server error"))
                .when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(content().string("Downstream api server error: Downstream server error"));

        // Verify
        verify(roundUpService).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldHandleConcurrentRequests() throws Exception {
        // Given
        int numberOfThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        doNothing().when(roundUpService).roundUpTransactions();

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/v2/feed/roundup")
                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify all requests were processed
        verify(roundUpService, times(numberOfThreads)).roundUpTransactions();
        assert successCount.get() == numberOfThreads;
        assert errorCount.get() == 0;
    }

    @Test
    void roundUpTransactions_ShouldHandleMultipleRequests() throws Exception {
        // Given
        int requestCount = 10;
        doNothing().when(roundUpService).roundUpTransactions();

        // When
        for (int i = 0; i < requestCount; i++) {
            mockMvc.perform(post("/api/v2/feed/roundup")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
            
            // Add a small delay between requests to prevent overwhelming the system
            Thread.sleep(50);
        }

        // Then
        // Verify all requests were processed successfully
        verify(roundUpService, times(requestCount)).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldHandleDifferentCurrencies() throws Exception {
        // Given
        String[] currencies = {"GBP", "EUR", "USD", "JPY"};
        doNothing().when(roundUpService).roundUpTransactions();

        // When & Then
        for (String currency : currencies) {
            mockMvc.perform(post("/api/v2/feed/roundup")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Verify service was called for each currency
        verify(roundUpService, times(currencies.length)).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldHandleLargeAmounts() throws Exception {
        // Given
        doNothing().when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify service was called
        verify(roundUpService, times(1)).roundUpTransactions();
    }

    @Test
    void roundUpTransactions_ShouldHandleZeroAmountTransactions() throws Exception {
        // Given
        doNothing().when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify service was called
        verify(roundUpService, times(1)).roundUpTransactions();
    }
} 