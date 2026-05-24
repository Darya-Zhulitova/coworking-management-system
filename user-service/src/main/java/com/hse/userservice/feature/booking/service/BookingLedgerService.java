package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingLedgerService {
    private final LedgerEntryRepository ledgerEntryRepository;
    private final Clock clock;

    public List<LedgerEntry> chargeBookings(Long membershipId, List<Booking> bookings, List<ResolvedCartItem> items) {
        if (bookings.size() != items.size()) {
            throw new IllegalArgumentException(
                    "Количество бронирований должно совпадать с количеством позиций в корзине.");
        }
        List<LedgerEntry> charges = new ArrayList<>();
        for (int index = 0; index < bookings.size(); index++) {
            Booking booking = bookings.get(index);
            ResolvedCartItem item = items.get(index);
            LedgerEntry charge = new LedgerEntry();
            charge.setMembershipId(membershipId);
            charge.setType(LedgerEntryType.BOOKING_CHARGE);
            charge.setAmount(-booking.getCost());
            charge.setComment(buildChargeComment(booking, item));
            charge.setReferenceId(booking.getId());
            charge.setTimestamp(LocalDateTime.now(clock));
            charges.add(charge);
        }
        return ledgerEntryRepository.saveAllAndFlush(charges);
    }

    public LedgerEntry refundUserCancellation(Long membershipId, Booking booking, long refundAmount) {
        if (refundAmount <= 0) {
            throw new IllegalArgumentException("Сумма возврата должна быть положительной.");
        }
        LedgerEntry refund = new LedgerEntry();
        refund.setMembershipId(membershipId);
        refund.setType(LedgerEntryType.BOOKING_USER_CANCELLATION_REFUND);
        refund.setAmount(refundAmount);
        refund.setComment(buildCancellationComment(booking, refundAmount));
        refund.setReferenceId(booking.getId());
        refund.setTimestamp(LocalDateTime.now(clock));
        return ledgerEntryRepository.saveAndFlush(refund);
    }

    private String buildChargeComment(Booking booking, ResolvedCartItem item) {
        return "Бронирование " + booking.getBookingNumber() + " · " + item.placeName() + " · " + item.date();
    }

    private String buildCancellationComment(Booking booking, long refundAmount) {
        return "Возврат по бронированию " + booking.getBookingNumber() + " · " + booking.getDate();
    }
}
