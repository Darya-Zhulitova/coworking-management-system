package com.hse.userservice.controller;

import com.hse.userservice.dto.request.CartCalculateRequestDto;
import com.hse.userservice.dto.request.CreateFromCartRequestDto;
import com.hse.userservice.dto.response.*;
import com.hse.userservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/api/coworkings/{coworkingId}/booking/init")
    public BookingInitResponseDto getInit(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return bookingService.getBookingInit(coworkingId, date);
    }

    @GetMapping("/api/coworkings/{coworkingId}/bookings")
    public List<BookingListItemDto> getBookings(@PathVariable Long coworkingId) {
        return bookingService.getBookings(coworkingId);
    }

    @PostMapping("/api/bookings/cart/calculate")
    public CartCalculationResponseDto calculate(@Valid @RequestBody CartCalculateRequestDto dto) {
        return bookingService.calculateCart(dto);
    }

    @PostMapping("/api/bookings/create-from-cart")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateFromCartResponseDto createFromCart(@Valid @RequestBody CreateFromCartRequestDto dto) {
        return bookingService.createFromCart(dto);
    }

    @PostMapping("/api/coworkings/{coworkingId}/bookings/{bookingId}/cancel")
    public CancelBookingResponseDto cancelBooking(@PathVariable Long coworkingId, @PathVariable Long bookingId) {
        return bookingService.cancelBooking(coworkingId, bookingId);
    }
}
