package com.hse.userservice.feature.balance.dto;

import com.hse.userservice.feature.balance.domain.PayRequestStatus;

import java.time.LocalDateTime;

public record PayRequestDto(
        Long id,
        Long amount,
        PayRequestStatus status,
        String userComment,
        String adminComment,
        LocalDateTime createdAt
) {
}
