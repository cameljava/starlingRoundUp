package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.starling.roundup.config.WireMockConfig;
import com.example.starling.roundup.exception.InsufficientBalanceException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.FeedItem;
import com.example.starling.roundup.model.SavingsGoal;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@Import(WireMockConfig.class)
class RoundUpServiceIntegrationTest {

    @Autowired
    private RoundUpService roundUpService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private TransactionFeedItemService transactionFeedItemService;

    private static final UUID ACCOUNT_UID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID CATEGORY_UID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final UUID SAVINGS_GOAL_UID = UUID.fromString("123e4567-e89b-12d3-a456-426614174003");

    @BeforeEach
    void setUp() {
        WireMock.reset();
        setupCommonStubs();
    }

    private void setupCommonStubs() {
        // Mock account response
        stubFor(get(urlPathMatching("/api/v2/accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("accounts.json")));

        // Mock savings goals response
        stubFor(get(urlPathMatching("/api/v2/account/.*/savings-goals"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("savings-goals.json")));

        // Mock transactions response
        stubFor(get(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("transactions.json")));
    }

    @Nested
    @DisplayName("Successful Scenarios")
    class SuccessfulScenarios {

        @Test
        @DisplayName("Should successfully round up transactions when balance is sufficient")
        void roundUpTransactions_SuccessfulScenario() {
            // Given
            stubFor(get(urlPathMatching("/api/v2/accounts/.*/balance"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("balance.json")));

            stubFor(put(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("transfer-response.json")));

            // When
            roundUpService.roundUpTransactions();

            // Then
            verifyAllApiCalls();
            verifyAccountAndSavingsGoal();
            verifyBalanceAndTransactions();
        }

        @Test
        @DisplayName("Should not transfer when no round-up amount is available")
        void roundUpTransactions_NoRoundUpAmount() {
            // Given
            stubFor(get(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("transactions-no-roundup.json")));

            // When
            roundUpService.roundUpTransactions();

            // Then
            verify(getRequestedFor(urlPathMatching("/api/v2/accounts")));
            verify(getRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals")));
            verify(getRequestedFor(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between")));
            verify(0, putRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*")));
        }
    }

    @Nested
    @DisplayName("Error Scenarios")
    class ErrorScenarios {

        @Test
        @DisplayName("Should throw InsufficientBalanceException when balance is too low")
        void roundUpTransactions_InsufficientBalance() {
            // Given
            stubFor(get(urlPathMatching("/api/v2/accounts/.*/balance"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("balance-insufficient.json")));

            // When/Then
            assertThrows(InsufficientBalanceException.class, () -> roundUpService.roundUpTransactions());
            verify(0, putRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*")));
        }

        @Test
        @DisplayName("Should create new savings goal when none exists")
        void roundUpTransactions_CreateNewSavingsGoal() {
            // Given
            stubFor(get(urlPathMatching("/api/v2/account/.*/savings-goals"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("savings-goals-empty.json")));

            stubFor(post(urlPathMatching("/api/v2/account/.*/savings-goals"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("create-savings-goal.json")));

            stubFor(get(urlPathMatching("/api/v2/accounts/.*/balance"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("balance.json")));

            stubFor(put(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("transfer-response.json")));

            // When
            roundUpService.roundUpTransactions();

            // Then
            verify(postRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals")));
            verifyAllApiCalls();
        }
    }

    private void verifyAllApiCalls() {
        verify(getRequestedFor(urlPathMatching("/api/v2/accounts")));
        verify(getRequestedFor(urlPathMatching("/api/v2/accounts/.*/balance")));
        verify(getRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals")));
        verify(getRequestedFor(urlPathMatching("/api/v2/feed/account/.*/category/.*/transactions-between")));
        verify(putRequestedFor(urlPathMatching("/api/v2/account/.*/savings-goals/.*/add-money/.*")));
    }

    private void verifyAccountAndSavingsGoal() {
        Account account = accountService.getDefaultAccount();
        assertNotNull(account);
        assertEquals(ACCOUNT_UID, account.accountUid());
        assertEquals(CATEGORY_UID, account.defaultCategory());

        SavingsGoal savingsGoal = goalService.getOrCreateSavingsGoal(ACCOUNT_UID);
        assertNotNull(savingsGoal);
        assertEquals(SAVINGS_GOAL_UID.toString(), savingsGoal.savingsGoalUid());
    }

    private void verifyBalanceAndTransactions() {
        CurrencyAndAmount balance = accountService.getEffectiveBalance(ACCOUNT_UID);
        assertTrue(balance.minorUnits() > 0);

        List<FeedItem> feedItems = transactionFeedItemService.getFeedItemsForDateRange(
                ACCOUNT_UID,
                CATEGORY_UID,
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );
        assertNotNull(feedItems);
        assertFalse(feedItems.isEmpty());
    }
} 