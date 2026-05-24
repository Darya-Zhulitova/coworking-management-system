package com.hse.userservice.feature.booking.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class BookingRequestIdGenerator {
    public String generate() {
        return "REQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
