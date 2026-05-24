package com.hse.userservice.feature.balance.controller;

import com.hse.userservice.feature.balance.dto.BalanceDetailsDto;
import com.hse.userservice.feature.balance.dto.CreatePayRequestRequest;
import com.hse.userservice.feature.balance.dto.PayRequestDto;
import com.hse.userservice.feature.balance.service.BalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memberships/{membershipId}")
public class BalanceController {
    private final BalanceService balanceService;

    @GetMapping("/balance")
    public BalanceDetailsDto getBalance(@PathVariable Long membershipId) {
        return balanceService.getBalanceDetails(membershipId);
    }

    @PostMapping("/pay-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public PayRequestDto create(@PathVariable Long membershipId, @Valid @RequestBody CreatePayRequestRequest dto) {
        return balanceService.create(membershipId, dto);
    }
}
