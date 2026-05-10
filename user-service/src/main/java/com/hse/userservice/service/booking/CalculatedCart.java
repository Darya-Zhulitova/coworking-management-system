package com.hse.userservice.service.booking;

import com.hse.userservice.dto.response.CartCalculationResponseDto;

import java.util.List;

public record CalculatedCart(
        CartCalculationResponseDto response,
        List<ResolvedCartItem> resolvedItems
) {
}
