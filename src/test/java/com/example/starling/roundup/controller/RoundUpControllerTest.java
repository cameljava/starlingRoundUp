package com.example.starling.roundup.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starling.roundup.StarlingRoundUpApplication;
import com.example.starling.roundup.service.RoundUpService;

@SpringBootTest(classes = StarlingRoundUpApplication.class)
@AutoConfigureMockMvc
public class RoundUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoundUpService roundUpService;

    @Test
    public void roundUpTransactions_ShouldReturnOk() throws Exception {
        // Given
        doNothing().when(roundUpService).roundUpTransactions();

        // When & Then
        mockMvc.perform(post("/api/v2/feed/roundup"))
                .andExpect(status().isOk());

        // Verify
        verify(roundUpService).roundUpTransactions();
    }
} 