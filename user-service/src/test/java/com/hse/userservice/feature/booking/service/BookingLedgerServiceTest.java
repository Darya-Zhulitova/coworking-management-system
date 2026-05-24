package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.integration.dto.BookingContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingLedgerServiceTest {
    private final LedgerEntryRepository ledgerEntryRepository = mock(LedgerEntryRepository.class);
    private final BookingLedgerService service = new BookingLedgerService(ledgerEntryRepository, BookingServiceUnitFixtures.CLOCK);

    @Test
    void chargeBookingsCreatesNegativeBookingChargeLedgerEntriesWithBookingReference() {
        Booking firstBooking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(1), 1_500L);
        Booking secondBooking = BookingServiceUnitFixtures.booking(2L, 11L, 101L, BookingServiceUnitFixtures.TODAY.plusDays(2), 2_500L);
        ResolvedCartItem firstItem = item(firstBooking, "Desk 1");
        ResolvedCartItem secondItem = item(secondBooking, "Room 1");
        when(ledgerEntryRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LedgerEntry> entries = service.chargeBookings(11L, List.of(firstBooking, secondBooking), List.of(firstItem, secondItem));

        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(LedgerEntry::getMembershipId).containsOnly(11L);
        assertThat(entries).extracting(LedgerEntry::getType).containsOnly(LedgerEntryType.BOOKING_CHARGE);
        assertThat(entries).extracting(LedgerEntry::getAmount).containsExactly(-1_500L, -2_500L);
        assertThat(entries).extracting(LedgerEntry::getReferenceId).containsExactly(1L, 2L);
        assertThat(entries).allSatisfy(entry -> assertThat(entry.getTimestamp()).isEqualTo(BookingServiceUnitFixtures.TODAY.atTime(10, 0)));
        assertThat(entries.getFirst().getComment()).contains("BR-2026-000001", "Desk 1", firstBooking.getDate().toString());
    }

    @Test
    void chargeBookingsRejectsMismatchedBookingsAndItemsCount() {
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(1), 1_500L);

        assertThatThrownBy(() -> service.chargeBookings(11L, List.of(booking), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Количество бронирований должно совпадать");

        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void refundUserCancellationCreatesPositiveRefundEntry() {
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(1), 1_500L);
        when(ledgerEntryRepository.saveAndFlush(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry refund = service.refundUserCancellation(11L, booking, 900L);

        assertThat(refund.getMembershipId()).isEqualTo(11L);
        assertThat(refund.getType()).isEqualTo(LedgerEntryType.BOOKING_USER_CANCELLATION_REFUND);
        assertThat(refund.getAmount()).isEqualTo(900L);
        assertThat(refund.getReferenceId()).isEqualTo(1L);
        assertThat(refund.getTimestamp()).isEqualTo(BookingServiceUnitFixtures.TODAY.atTime(10, 0));
        assertThat(refund.getComment()).contains("BR-2026-000001");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue()).isSameAs(refund);
    }

    @Test
    void refundUserCancellationRejectsNonPositiveRefund() {
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(1), 1_500L);

        assertThatThrownBy(() -> service.refundUserCancellation(11L, booking, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("положительной");

        verify(ledgerEntryRepository, never()).saveAndFlush(any());
    }

    private ResolvedCartItem item(Booking booking, String placeName) {
        BookingContext.Tariff tariff = BookingServiceUnitFixtures.tariff(booking.getTariffId(), booking.getPricePerDay(), true);
        return new ResolvedCartItem(
                booking.getPlaceId(),
                placeName,
                booking.getDate(),
                "Floor",
                "Desk",
                tariff,
                booking.getCost(),
                true
        );
    }
}
