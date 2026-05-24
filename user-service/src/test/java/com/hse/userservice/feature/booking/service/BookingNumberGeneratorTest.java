package com.hse.userservice.feature.booking.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingNumberGeneratorTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final BookingNumberGenerator generator = new BookingNumberGenerator(jdbcTemplate, BookingServiceUnitFixtures.CLOCK);

    @Test
    void generateUsesCurrentYearAndZeroPaddedDatabaseSequence() {
        when(jdbcTemplate.queryForObject("select nextval('booking_number_seq')", Long.class)).thenReturn(42L);

        String bookingNumber = generator.generate();

        assertThat(bookingNumber).isEqualTo("BR-2026-000042");
    }

    @Test
    void generateFailsWhenSequenceReturnsNull() {
        when(jdbcTemplate.queryForObject("select nextval('booking_number_seq')", Long.class)).thenReturn(null);

        assertThatThrownBy(generator::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Не удалось сформировать номер бронирования");
    }
}
