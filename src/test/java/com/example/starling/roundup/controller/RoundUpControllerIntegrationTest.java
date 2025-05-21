package com.example.starling.roundup.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.AccountsResponse;
import com.example.starling.roundup.model.Balance;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.FeedItem;
import com.example.starling.roundup.model.FeedItems;
import com.example.starling.roundup.model.SavingsGoal;
import com.example.starling.roundup.model.SavingsGoalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class RoundUpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID accountUid;
    private UUID categoryUid;
    private UUID savingsGoalUid;
    private Account account;
    private FeedItem feedItem;
    private SavingsGoal savingsGoal;

    @BeforeEach
    void setUp() {
        accountUid = UUID.randomUUID();
        categoryUid = UUID.randomUUID();
        savingsGoalUid = UUID.randomUUID();
        
        // Setup test data
        account = new Account(accountUid, categoryUid, "Personal", "GBP");
        feedItem = new FeedItem(
            UUID.randomUUID(),
            categoryUid,
            new CurrencyAndAmount("GBP", 450L),
            new CurrencyAndAmount("GBP", 450L),
            "OUT",
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            "FASTER_PAYMENTS_OUT",
            "SETTLED"
        );
        savingsGoal = new SavingsGoal(
            savingsGoalUid.toString(),
            "Round Up Savings",
            "GBP",
            new CurrencyAndAmount("GBP", 0L)
        );
    }

    @Test
    void roundUpTransactions_ShouldProcessSuccessfully() throws Exception {
        // Mock accounts API
        stubFor(get(urlPathEqualTo("/api/v2/accounts"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new AccountsResponse(List.of(account))))));

        // Mock balance API
        stubFor(get(urlPathMatching("/api/v2/accounts/.*/balance"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new Balance(
                    new CurrencyAndAmount("GBP", 1000L),
                    new CurrencyAndAmount("GBP", 1000L),
                    new CurrencyAndAmount("GBP", 0L),
                    new CurrencyAndAmount("GBP", 0L),
                    new CurrencyAndAmount("GBP", 1000L)
                )))));

        // Mock savings goals API
        stubFor(get(urlPathMatching("/api/v2/account/.*/savings-goals"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new SavingsGoalResponse(List.of(savingsGoal))))));

        // Mock transactions API
        stubFor(get(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new FeedItems(List.of(feedItem))))));

        // Mock transfer to savings goal API
        stubFor(put(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"transferUid\": \"" + UUID.randomUUID() + "\"}")));

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify all API calls were made
        verify(getRequestedFor(urlPathEqualTo("/api/v2/accounts")));
        verify(getRequestedFor(urlPathMatching("/api/v2/accounts/.*/balance")));
        verify(getRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals")));
        verify(getRequestedFor(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between")));
        verify(putRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*")));
    }

    @Test
    void roundUpTransactions_ShouldHandleInsufficientBalance() throws Exception {
        // Mock accounts API
        stubFor(get(urlPathEqualTo("/api/v2/accounts"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new AccountsResponse(List.of(account))))));

        // Mock balance API with insufficient balance
        stubFor(get(urlPathMatching("/api/v2/accounts/.*/balance"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new Balance(
                    new CurrencyAndAmount("GBP", 10L),
                    new CurrencyAndAmount("GBP", 10L),
                    new CurrencyAndAmount("GBP", 0L),
                    new CurrencyAndAmount("GBP", 0L),
                    new CurrencyAndAmount("GBP", 10L)
                )))));

        // Mock savings goals API
        stubFor(get(urlPathMatching("/api/v2/account/.*/savings-goals"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new SavingsGoalResponse(List.of(savingsGoal))))));

        // Mock transactions API
        stubFor(get(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(new FeedItems(List.of(feedItem))))));

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());

        // Verify API calls were made but transfer was not attempted
        verify(getRequestedFor(urlPathEqualTo("/api/v2/accounts")));
        verify(getRequestedFor(urlPathMatching("/api/v2/accounts/.*/balance")));
        verify(getRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals")));
        verify(getRequestedFor(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between")));
        verify(0, putRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*")));
    }

    @Test
    void roundUpTransactions_ShouldHandleDownstreamError() throws Exception {
        // Mock accounts API with error
        stubFor(get(urlPathEqualTo("/api/v2/accounts"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"error\": \"Internal Server Error\"}")));

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway());

        // Verify only the first API call was attempted
        verify(getRequestedFor(urlPathEqualTo("/api/v2/accounts")));
        verify(0, getRequestedFor(urlPathMatching("/api/v2/accounts/.*/balance")));
        verify(0, getRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals")));
        verify(0, getRequestedFor(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between")));
        verify(0, putRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*")));
    }
} 