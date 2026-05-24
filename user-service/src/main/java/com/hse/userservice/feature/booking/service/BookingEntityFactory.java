package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingEntityFactory {
    public Booking fromCartItem(Long membershipId, String requestId, String bookingNumber, ResolvedCartItem item) {
        Booking booking = new Booking();
        booking.setPlaceId(item.placeId());
        booking.setMembershipId(membershipId);
        booking.setDate(item.date());
        booking.setCost(item.finalPrice());
        booking.setActive(true);
        booking.setStatus(BookingStatus.ACTUAL);
        booking.setBookingNumber(bookingNumber);
        booking.setRequestId(requestId);
        booking.setTariffId(item.tariff().id());
        booking.setPricePerDay(item.tariff().pricePerDay());
        booking.setFullRefundHoursBefore(item.tariff().fullRefundHoursBefore());
        booking.setLateCancellationRefundPercent(item.tariff().lateCancellationRefundPercent());
        booking.setCancellationCompensationCoefficient(item.tariff().cancellationCompensationCoefficient());
        booking.setDayClosureCompensationCoefficient(item.tariff().dayClosureCompensationCoefficient());
        booking.setMembershipBlockCompensationCoefficient(item.tariff().membershipBlockCompensationCoefficient());
        return booking;
    }
}
