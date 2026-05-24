package com.hse.userservice.feature.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BookingNumberGenerator {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;


    public String generate() {
        Long sequenceValue = jdbcTemplate.queryForObject("select nextval('booking_number_seq')", Long.class);
        if (sequenceValue == null) {
            throw new IllegalStateException("Не удалось сформировать номер бронирования.");
        }
        int year = LocalDate.now(clock).getYear();
        return "BR-%d-%06d".formatted(year, sequenceValue);
    }
}
