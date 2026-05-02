package com.hse.userservice.controller;

import com.hse.userservice.dto.request.CreatePayRequestDto;
import com.hse.userservice.dto.response.BalanceDetailsDto;
import com.hse.userservice.dto.response.PayRequestDto;
import com.hse.userservice.service.PayRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PayRequestController {
    private final PayRequestService payRequestService;

    @GetMapping("/api/coworkings/{coworkingId}/balance")
    public BalanceDetailsDto getBalance(@PathVariable Long coworkingId) {
        return payRequestService.getBalanceDetails(coworkingId);
    }

    @PostMapping("/api/pay-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public PayRequestDto create(@Valid @RequestBody CreatePayRequestDto dto) {
        return payRequestService.create(dto);
    }
}
