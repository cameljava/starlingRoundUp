package com.example.starling.roundup.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.SavingsGoal;
import com.example.starling.roundup.model.SavingsGoalRequest;
import com.example.starling.roundup.model.SavingsGoalResponse;
import com.example.starling.roundup.model.SavingsGoalTransferResponse;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private GoalServiceImpl goalService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        goalService = new GoalServiceImpl(restTemplate);
    }

    @Test
    void getOrCreateSavingsGoal_existingGoal_returnsExisting() {
        UUID accountUid = UUID.randomUUID();
        SavingsGoal existingGoal = new SavingsGoal("goal-uid", "Round Up Savings", "GBP", new CurrencyAndAmount("GBP", 100L));
        SavingsGoalResponse response = new SavingsGoalResponse(List.of(existingGoal));
        when(restTemplate.getForObject("/api/v2/account/" + accountUid + "/savings-goals", SavingsGoalResponse.class))
                .thenReturn(response);

        SavingsGoal result = goalService.getOrCreateSavingsGoal(accountUid);

        assertEquals(existingGoal, result);
        verify(restTemplate, never()).postForObject(anyString(), any(SavingsGoalRequest.class), eq(SavingsGoal.class));
    }

    @Test
    void getOrCreateSavingsGoal_noExistingGoal_createsNew() {
        UUID accountUid = UUID.randomUUID();
        SavingsGoalRequest savingsGoalRequest = new SavingsGoalRequest(
                goalService.ROUND_UP_SAVINGS_GOAL_NAME,
                "GBP",
                new CurrencyAndAmount("GBP", 100000L)
        );
        SavingsGoalResponse response = new SavingsGoalResponse(Collections.emptyList());
        SavingsGoal newGoal = new SavingsGoal("new-uid", goalService.ROUND_UP_SAVINGS_GOAL_NAME, "GBP", new CurrencyAndAmount("GBP", 1000L));
        when(restTemplate.getForObject("/api/v2/account/" + accountUid + "/savings-goals", SavingsGoalResponse.class))
                .thenReturn(response);
        when(restTemplate.postForObject("/api/v2/account/" + accountUid + "/savings-goals", savingsGoalRequest, SavingsGoal.class))
                .thenReturn(newGoal);

        SavingsGoal result = goalService.getOrCreateSavingsGoal(accountUid);

        assertEquals(newGoal, result);
    }

    @Test
    void getOrCreateSavingsGoal_nullResponse_throwsException() {
        when(restTemplate.getForObject(anyString(), eq(SavingsGoalResponse.class))).thenReturn(null);

        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class, ()
                -> goalService.getOrCreateSavingsGoal(UUID.randomUUID())
        );
        assertNotNull(exception);
    }

    @Test
    void getOrCreateSavingsGoal_nullList_throwsException() {
        SavingsGoalResponse response = new SavingsGoalResponse(null);
        when(restTemplate.getForObject(anyString(), eq(SavingsGoalResponse.class))).thenReturn(response);

        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class, ()
                -> goalService.getOrCreateSavingsGoal(UUID.randomUUID())
        );
        assertNotNull(exception);
    }

    @Test
    void transferToSavingsGoal_success_returnsTransferId() {
        ResponseEntity<SavingsGoalTransferResponse> responseEntity
                = ResponseEntity.ok(new SavingsGoalTransferResponse(true, "transfer-uid"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(SavingsGoalTransferResponse.class)))
                .thenReturn(responseEntity);

        String result = goalService.transferToSavingsGoal(UUID.randomUUID(), UUID.randomUUID(), 500L);

        assertEquals("transfer-uid", result);
    }

    @Test
    void transferToSavingsGoal_nullBody_throwsException() {
        ResponseEntity<SavingsGoalTransferResponse> responseEntity = ResponseEntity.ok(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(SavingsGoalTransferResponse.class)))
                .thenReturn(responseEntity);

        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class, ()
                -> goalService.transferToSavingsGoal(UUID.randomUUID(), UUID.randomUUID(), 200L)
        );
        assertNotNull(exception);
    }

    @Test
    void transferToSavingsGoal_nullTransferUid_throwsException() {
        ResponseEntity<SavingsGoalTransferResponse> responseEntity
                = ResponseEntity.ok(new SavingsGoalTransferResponse(true, null));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(SavingsGoalTransferResponse.class)))
                .thenReturn(responseEntity);

        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class, ()
                -> goalService.transferToSavingsGoal(UUID.randomUUID(), UUID.randomUUID(), 300L)
        );
        assertNotNull(exception);
    }
}
