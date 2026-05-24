package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartValidationPolicy {
    public List<String> validate(List<BookingCartItemRequestDto> items, BookingSnapshotContext context) {
        return List.of();
    }
}
