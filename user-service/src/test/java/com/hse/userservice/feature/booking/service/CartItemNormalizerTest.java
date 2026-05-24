package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemNormalizerTest {
    private final CartItemNormalizer normalizer = new CartItemNormalizer();

    @Test
    void returnsEmptyListForNullInput() {
        assertThat(normalizer.normalize(null)).isEmpty();
    }

    @Test
    void keepsOnlyLastItemForSamePlaceAndDateAndPreservesFirstKeyOrder() {
        LocalDate date = BookingServiceUnitFixtures.TODAY.plusDays(1);
        BookingCartItemRequestDto first = new BookingCartItemRequestDto(100L, date);
        BookingCartItemRequestDto duplicate = new BookingCartItemRequestDto(100L, date);
        BookingCartItemRequestDto another = new BookingCartItemRequestDto(101L, date);

        List<BookingCartItemRequestDto> normalized = normalizer.normalize(List.of(first, another, duplicate));

        assertThat(normalized).containsExactly(duplicate, another);
    }
}
