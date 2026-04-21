package com.hse.userservice.dto.response;

import com.hse.userservice.domain.payrequest.PayRequestStatus;

import java.time.LocalDateTime;

public record PayRequestDto(
        Long id,
        Long coworkingId,
        Long amount,
        PayRequestStatus status,
        String userComment,
        String adminComment,
        LocalDateTime createdAt
) {
}
