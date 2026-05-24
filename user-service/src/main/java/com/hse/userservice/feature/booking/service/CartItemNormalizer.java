package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class CartItemNormalizer {
    public List<BookingCartItemRequestDto> normalize(List<BookingCartItemRequestDto> items) {
        LinkedHashMap<String, BookingCartItemRequestDto> unique = new LinkedHashMap<>();
        for (BookingCartItemRequestDto item : items == null ? List.<BookingCartItemRequestDto>of() : items) {
            unique.put(BookingSnapshotContextFactory.key(item.placeId(), item.date()), item);
        }
        return new ArrayList<>(unique.values());
    }
}
