package com.example.starling.roundup.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.starling.roundup.service.RoundUpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/feed")
@Tag(name = "RoundUp", description = "RoundUp transaction management APIs")
public class RoundUpController {

    private final RoundUpService roundUpService;

    public RoundUpController(RoundUpService roundUpService) {
        this.roundUpService = roundUpService;
    }

    @Operation(
        summary = "Round up transactions",
        description = "Rounds up all transactions to the nearest pound and transfers the difference to the savings goal"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Transactions rounded up successfully"
    )
    @PostMapping("/roundup")
    public ResponseEntity<Void> roundUpTransactions() {
        roundUpService.roundUpTransactions();
        return ResponseEntity.ok().build();
    }
}
