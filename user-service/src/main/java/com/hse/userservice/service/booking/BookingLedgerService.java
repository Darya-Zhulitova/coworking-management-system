package com.hse.userservice.service.booking;

import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.domain.ledger.LedgerEntryType;
import com.hse.userservice.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingLedgerService {
    private final LedgerEntryRepository ledgerEntryRepository;
    private final Clock clock;

    public LedgerEntry chargeBooking(Long membershipId, Long coworkingId, long amount, List<ResolvedCartItem> items) {
        LedgerEntry charge = new LedgerEntry();
        charge.setMembershipId(membershipId);
        charge.setCoworkingId(coworkingId);
        charge.setType(LedgerEntryType.BOOKING_CHARGE);
        charge.setAmount(-amount);
        charge.setName("Оплата бронирования");
        charge.setComment(buildChargeComment(items));
        charge.setTimestamp(LocalDateTime.now(clock));
        return ledgerEntryRepository.save(charge);
    }

    public LedgerEntry refundUserCancellation(Long membershipId, Long coworkingId, Booking booking, long refundAmount) {
        LedgerEntry refund = new LedgerEntry();
        refund.setMembershipId(membershipId);
        refund.setCoworkingId(coworkingId);
        refund.setType(LedgerEntryType.CANCELLATION_REFUND);
        refund.setAmount(refundAmount);
        refund.setName("Возврат по отмене бронирования");
        refund.setComment(buildCancellationComment(booking, refundAmount));
        refund.setTimestamp(LocalDateTime.now(clock));
        return ledgerEntryRepository.save(refund);
    }

    private String buildChargeComment(List<ResolvedCartItem> items) {
        return items.stream().map(item -> item.placeName() + " - " + item.date()).collect(Collectors.joining(", "));
    }

    private String buildCancellationComment(Booking booking, long refundAmount) {
        return "Бронирование " + booking.getRequestId() + " · placeId=" + booking.getPlaceId() + " · " + booking.getDate() + " · refund=" + refundAmount;
    }
}
