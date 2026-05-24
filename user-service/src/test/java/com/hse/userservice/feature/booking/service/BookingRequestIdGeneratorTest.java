package com.hse.userservice.feature.booking.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequestIdGeneratorTest {
    private final BookingRequestIdGenerator generator = new BookingRequestIdGenerator();

    @Test
    void generateReturnsStablePublicFormatWithoutUuidDashes() {
        String requestId = generator.generate();

        assertThat(requestId).startsWith("REQ-");
        assertThat(requestId).hasSize(16);
        assertThat(requestId.substring(4)).matches("[0-9A-F]{12}");
    }
}
