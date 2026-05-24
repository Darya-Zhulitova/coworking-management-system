package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.dto.CartCalculationResponseDto;

import java.util.List;

public record CalculatedCart(
        CartCalculationResponseDto response,
        List<ResolvedCartItem> resolvedItems
) {
}
