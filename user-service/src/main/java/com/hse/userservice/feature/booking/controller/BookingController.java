package com.hse.userservice.feature.booking.controller;

import com.hse.userservice.feature.booking.dto.*;
import com.hse.userservice.feature.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memberships/{membershipId}")
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/booking/init")
    public BookingInitResponseDto getInit(
            @PathVariable Long membershipId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return bookingService.getBookingInit(membershipId, date);
    }

    @GetMapping("/places/{placeId}/availability")
    public List<PlaceAvailabilityDayDto> getPlaceAvailability(
            @PathVariable Long membershipId,
            @PathVariable Long placeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return bookingService.getPlaceAvailability(membershipId, placeId, from, to);
    }

    @GetMapping("/bookings")
    public List<BookingListItemDto> getBookings(@PathVariable Long membershipId) {
        return bookingService.getBookings(membershipId);
    }

    @PostMapping("/bookings/cart/calculate")
    public CartCalculationResponseDto calculate(
            @PathVariable Long membershipId,
            @Valid @RequestBody CartCalculateRequestDto dto
    ) {
        return bookingService.calculateCart(membershipId, dto);
    }

    @PostMapping("/bookings/create-from-cart")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateFromCartResponseDto createFromCart(
            @PathVariable Long membershipId,
            @Valid @RequestBody CreateFromCartRequestDto dto
    ) {
        return bookingService.createFromCart(membershipId, dto);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public CancelBookingResponseDto cancelBooking(@PathVariable Long membershipId, @PathVariable Long bookingId) {
        return bookingService.cancelBooking(membershipId, bookingId);
    }
}
